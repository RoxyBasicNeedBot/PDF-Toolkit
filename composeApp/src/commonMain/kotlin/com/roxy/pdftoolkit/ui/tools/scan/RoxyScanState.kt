package com.roxy.pdftoolkit.ui.tools.scan

import com.roxy.pdftoolkit.engine.scanner.RoxyDocumentQuad
import com.roxy.pdftoolkit.engine.scanner.RoxyScannedPage

sealed interface RoxyScanState {
    data class CameraCapturing(
        val pages: List<RoxyScannedPage> = emptyList(),
        val liveQuad: RoxyDocumentQuad = RoxyDocumentQuad.Default
    ) : RoxyScanState

    data class ReviewingPage(
        val activePageIndex: Int,
        val page: RoxyScannedPage,
        val totalPages: Int
    ) : RoxyScanState

    data class Compiling(
        val currentPage: Int,
        val totalPages: Int,
        val progressPercent: Float
    ) : RoxyScanState

    data class Success(
        val outputUri: String,
        val pageCount: Int
    ) : RoxyScanState

    data class Error(
        val message: String
    ) : RoxyScanState
}
