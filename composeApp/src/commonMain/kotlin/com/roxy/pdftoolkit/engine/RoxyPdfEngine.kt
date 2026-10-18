package com.roxy.pdftoolkit.engine

import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import com.roxy.pdftoolkit.engine.model.RoxyPdfDocument
import kotlinx.coroutines.flow.Flow

/**
 * Platform-independent abstraction for all PDF operations.
 * Android implements this with PdfRenderer and PdfBox-Android.
 * Desktop (Windows/macOS/Linux) will implement this with Apache PDFBox or MuPDF.
 */
interface RoxyPdfEngine {
    /**
     * Inspect a PDF from the given URI / path and return its metadata.
     */
    suspend fun getDocumentMetadata(uri: String): Result<RoxyPdfDocument>

    /**
     * Total number of pages in the given document.
     */
    suspend fun getPageCount(uri: String): Int

    /**
     * Render a thumbnail of a specific page at the requested maximum width.
     */
    suspend fun renderPageThumbnail(uri: String, pageIndex: Int, targetWidth: Int = 300): ByteArray?

    /**
     * Merge multiple PDF files into one output file.
     * Emits progress flow during multi-file processing.
     */
    fun mergePdfs(inputUris: List<String>, outputUri: String): Flow<RoxyOperationResult>

    /**
     * Split a PDF into one or more output files based on ranges.
     */
    fun splitPdf(inputUri: String, pageRanges: List<IntRange>, outputDirectoryUri: String): Flow<RoxyOperationResult>

    /**
     * Check if document is password protected.
     */
    suspend fun isPasswordProtected(uri: String): Boolean
}
