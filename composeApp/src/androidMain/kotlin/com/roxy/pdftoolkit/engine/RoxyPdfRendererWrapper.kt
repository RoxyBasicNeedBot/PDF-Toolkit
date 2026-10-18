package com.roxy.pdftoolkit.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Thread-safe wrapper around Android's native [PdfRenderer].
 *
 * [PdfRenderer] is strictly non-thread-safe and only allows one open page at a time.
 * This class protects all operations with a dedicated [Mutex].
 */
class RoxyPdfRendererWrapper(
    private val context: Context
) {
    private val mutex = Mutex()

    suspend fun getPageCount(uriString: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                val uri = Uri.parse(uriString)
                pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withLock 0
                renderer = PdfRenderer(pfd)
                renderer.pageCount
            } catch (e: Exception) {
                0
            } finally {
                renderer?.close()
                pfd?.close()
            }
        }
    }

    suspend fun renderPageToJpeg(
        uriString: String,
        pageIndex: Int,
        targetWidth: Int = 300
    ): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null
            try {
                val uri = Uri.parse(uriString)
                pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withLock null
                renderer = PdfRenderer(pfd)
                if (pageIndex !in 0 until renderer.pageCount) return@withLock null

                page = renderer.openPage(pageIndex)
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                bitmap.recycle()
                stream.toByteArray()
            } catch (e: Exception) {
                null
            } finally {
                page?.close()
                renderer?.close()
                pfd?.close()
            }
        }
    }
}
