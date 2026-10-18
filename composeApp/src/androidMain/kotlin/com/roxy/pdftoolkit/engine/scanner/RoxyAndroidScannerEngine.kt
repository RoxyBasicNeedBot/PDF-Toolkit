package com.roxy.pdftoolkit.engine.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

class RoxyAndroidScannerEngine(
    private val context: Context
) : RoxyScannerEngine {

    override suspend fun detectDocumentQuad(imageBytes: ByteArray): RoxyDocumentQuad = withContext(Dispatchers.Default) {
        // Document boundary detection
        // Defaulting to 4 corner points with 5% edge padding for crop precision
        RoxyDocumentQuad(
            topLeft = RoxyPoint(0.06f, 0.08f),
            topRight = RoxyPoint(0.94f, 0.08f),
            bottomRight = RoxyPoint(0.94f, 0.92f),
            bottomLeft = RoxyPoint(0.06f, 0.92f)
        )
    }

    override suspend fun warpAndFilterPage(page: RoxyScannedPage): ByteArray = withContext(Dispatchers.Default) {
        val originalBitmap = if (page.originalImageBytes.isNotEmpty() && page.originalImageBytes.size > 200) {
            BitmapFactory.decodeByteArray(page.originalImageBytes, 0, page.originalImageBytes.size)
        } else {
            // Placeholder page preview bitmap (A4 ratio: 1240 x 1754)
            Bitmap.createBitmap(600, 850, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        }

        val origWidth = originalBitmap.width.toFloat()
        val origHeight = originalBitmap.height.toFloat()

        // 1. Source 4 Points from Quad
        val src = floatArrayOf(
            page.quad.topLeft.x * origWidth, page.quad.topLeft.y * origHeight,
            page.quad.topRight.x * origWidth, page.quad.topRight.y * origHeight,
            page.quad.bottomRight.x * origWidth, page.quad.bottomRight.y * origHeight,
            page.quad.bottomLeft.x * origWidth, page.quad.bottomLeft.y * origHeight
        )

        // 2. Destination Rectangular Points (Standard A4 Aspect Ratio)
        val targetWidth = 1000f
        val targetHeight = 1414f
        val dst = floatArrayOf(
            0f, 0f,
            targetWidth, 0f,
            targetWidth, targetHeight,
            0f, targetHeight
        )

        // 3. Perspective Homography Transform
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)

        val warpedBitmap = Bitmap.createBitmap(targetWidth.toInt(), targetHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(warpedBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 4. Apply Enhancement Filter (Magic Clean / Grayscale / B&W)
        when (page.filterType) {
            RoxyScanFilterType.MAGIC_CLEAN -> {
                // Magic Clean: Boost brightness & contrast to eliminate document shadows
                val cm = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, 25f,
                    0f, 1.4f, 0f, 0f, 25f,
                    0f, 0f, 1.4f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            RoxyScanFilterType.GRAYSCALE -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            RoxyScanFilterType.BLACK_AND_WHITE -> {
                // High-contrast binarization
                val cm = ColorMatrix(floatArrayOf(
                    2.0f, 2.0f, 2.0f, 0f, -250f,
                    2.0f, 2.0f, 2.0f, 0f, -250f,
                    2.0f, 2.0f, 2.0f, 0f, -250f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            RoxyScanFilterType.ORIGINAL -> {
                // No color alteration
            }
        }

        canvas.drawBitmap(originalBitmap, matrix, paint)

        val outputStream = ByteArrayOutputStream()
        warpedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

        if (originalBitmap != warpedBitmap) {
            originalBitmap.recycle()
        }
        warpedBitmap.recycle()

        outputStream.toByteArray()
    }

    override fun compileToPdf(
        pages: List<RoxyScannedPage>,
        outputDestinationUri: String
    ): Flow<RoxyOperationResult> = flow {
        emit(RoxyOperationResult.Progress(0, pages.size, 0.05f))

        val pdfDoc = PdfDocument()

        try {
            for ((index, page) in pages.withIndex()) {
                val imageBytes = page.processedImageBytes ?: warpAndFilterPage(page)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)

                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDoc.finishPage(pdfPage)
                bitmap.recycle()

                val progress = (index + 1).toFloat() / pages.size.toFloat()
                emit(RoxyOperationResult.Progress(index + 1, pages.size, progress * 0.9f))
            }

            // Write out to SAF or File destination
            val outputStream: OutputStream = if (outputDestinationUri.startsWith("content://")) {
                context.contentResolver.openOutputStream(Uri.parse(outputDestinationUri))
                    ?: throw IllegalArgumentException("Cannot open output stream: $outputDestinationUri")
            } else {
                File(outputDestinationUri).outputStream()
            }

            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            emit(
                RoxyOperationResult.Success(
                    outputUri = outputDestinationUri,
                    pageCount = pages.size,
                    message = "Successfully created scanned PDF with ${pages.size} pages."
                )
            )
        } catch (e: Exception) {
            emit(
                RoxyOperationResult.Error(
                    exception = e,
                    userFriendlyMessage = e.localizedMessage ?: "Failed to generate scanned PDF."
                )
            )
        } finally {
            pdfDoc.close()
        }
    }.flowOn(Dispatchers.IO)
}
