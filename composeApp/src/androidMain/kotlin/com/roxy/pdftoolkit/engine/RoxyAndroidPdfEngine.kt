package com.roxy.pdftoolkit.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import com.roxy.pdftoolkit.engine.model.RoxyPdfDocument
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class RoxyAndroidPdfEngine(
    private val context: Context,
    private val rendererWrapper: RoxyPdfRendererWrapper = RoxyPdfRendererWrapper(context)
) : RoxyPdfEngine {

    override suspend fun getDocumentMetadata(uri: String): Result<RoxyPdfDocument> = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            var displayName = "document.pdf"
            var sizeBytes = 0L

            context.contentResolver.query(parsedUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }

            val pageCount = rendererWrapper.getPageCount(uri)
            val thumbnail = rendererWrapper.renderPageToJpeg(uri, 0, targetWidth = 240)

            Result.success(
                RoxyPdfDocument(
                    uri = uri,
                    displayName = displayName,
                    sizeBytes = sizeBytes,
                    pageCount = pageCount,
                    thumbnailBytes = thumbnail
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPageCount(uri: String): Int =
        rendererWrapper.getPageCount(uri)

    override suspend fun renderPageThumbnail(uri: String, pageIndex: Int, targetWidth: Int): ByteArray? =
        rendererWrapper.renderPageToJpeg(uri, pageIndex, targetWidth)

    override fun mergePdfs(inputUris: List<String>, outputUri: String): Flow<RoxyOperationResult> = flow {
        emit(RoxyOperationResult.Progress(0, inputUris.size, 0.05f))

        val merger = PDFMergerUtility()
        val tempFiles = mutableListOf<File>()
        val streamsToClose = mutableListOf<InputStream>()

        try {
            var totalPagesCount = 0
            for ((index, uriStr) in inputUris.withIndex()) {
                val uri = Uri.parse(uriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Could not open input file: $uriStr")
                streamsToClose.add(inputStream)
                merger.addSource(inputStream)

                totalPagesCount += rendererWrapper.getPageCount(uriStr)
                val progress = 0.1f + 0.5f * ((index + 1).toFloat() / inputUris.size.toFloat())
                emit(RoxyOperationResult.Progress(index + 1, inputUris.size, progress))
            }

            // Output destination handling
            val outputStream: OutputStream = if (outputUri.startsWith("content://")) {
                context.contentResolver.openOutputStream(Uri.parse(outputUri))
                    ?: throw IllegalArgumentException("Could not open output destination: $outputUri")
            } else {
                File(outputUri).outputStream()
            }

            merger.destinationStream = outputStream

            // Memory-efficient temp buffer setting prevents Android OOM on large files
            val tempDir = File(context.cacheDir, "roxy_pdfbox_temp").apply { mkdirs() }
            val memSetting = MemoryUsageSetting.setupTempFileOnly().setTempDir(tempDir)

            emit(RoxyOperationResult.Progress(inputUris.size, inputUris.size, 0.85f))
            merger.mergeDocuments(memSetting)
            outputStream.flush()
            outputStream.close()

            emit(
                RoxyOperationResult.Success(
                    outputUri = outputUri,
                    pageCount = totalPagesCount,
                    message = "Successfully merged ${inputUris.size} documents."
                )
            )
        } catch (e: Exception) {
            emit(
                RoxyOperationResult.Error(
                    exception = e,
                    userFriendlyMessage = e.localizedMessage ?: "Failed to merge PDF files."
                )
            )
        } finally {
            streamsToClose.forEach { runCatching { it.close() } }
            tempFiles.forEach { runCatching { it.delete() } }
        }
    }.flowOn(Dispatchers.IO)

    override fun splitPdf(
        inputUri: String,
        pageRanges: List<IntRange>,
        outputDirectoryUri: String
    ): Flow<RoxyOperationResult> = flow {
        // Implementation for Phase 1 Split
        emit(RoxyOperationResult.Error(UnsupportedOperationException(), "Split will be available in Phase 1."))
    }.flowOn(Dispatchers.IO)

    override suspend fun isPasswordProtected(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                PDDocument.load(stream).use { doc ->
                    doc.isEncrypted
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
