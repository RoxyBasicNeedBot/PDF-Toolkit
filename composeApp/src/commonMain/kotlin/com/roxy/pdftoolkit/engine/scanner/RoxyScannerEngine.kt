package com.roxy.pdftoolkit.engine.scanner

import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import kotlinx.coroutines.flow.Flow

/**
 * Platform-independent abstraction for document scanning, edge detection,
 * perspective warping, and Magic Clean filtering.
 */
interface RoxyScannerEngine {
    /**
     * Analyzes image bytes and returns the 4 detected document corner coordinates.
     */
    suspend fun detectDocumentQuad(imageBytes: ByteArray): RoxyDocumentQuad

    /**
     * Warps the document polygon into a rectangular page and applies the selected filter.
     */
    suspend fun warpAndFilterPage(page: RoxyScannedPage): ByteArray

    /**
     * Compiles all scanned and processed pages into a multi-page PDF file.
     */
    fun compileToPdf(pages: List<RoxyScannedPage>, outputDestinationUri: String): Flow<RoxyOperationResult>
}
