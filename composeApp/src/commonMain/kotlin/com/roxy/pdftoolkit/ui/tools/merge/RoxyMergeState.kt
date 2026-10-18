package com.roxy.pdftoolkit.ui.tools.merge

import com.roxy.pdftoolkit.engine.model.RoxyPdfDocument

sealed interface RoxyMergeState {
    data object EmptySelection : RoxyMergeState

    data class FilesSelected(
        val files: List<RoxyPdfDocument>,
        val totalPages: Int = 0
    ) : RoxyMergeState

    data class Processing(
        val currentPage: Int,
        val totalPages: Int,
        val progressPercent: Float
    ) : RoxyMergeState

    data class Success(
        val outputUri: String,
        val mergedPages: Int,
        val outputSizeBytes: Long
    ) : RoxyMergeState

    data class Error(
        val message: String
    ) : RoxyMergeState
}
