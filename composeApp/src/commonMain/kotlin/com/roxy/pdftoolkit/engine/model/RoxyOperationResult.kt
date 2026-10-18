package com.roxy.pdftoolkit.engine.model

/**
 * Result of any Roxy PDF operation.
 */
sealed interface RoxyOperationResult {
    data class Success(
        val outputUri: String,
        val pageCount: Int,
        val fileSizeBytes: Long = 0L,
        val message: String? = null
    ) : RoxyOperationResult

    data class Error(
        val exception: Throwable,
        val userFriendlyMessage: String
    ) : RoxyOperationResult

    data class Progress(
        val currentPage: Int,
        val totalPages: Int,
        val percent: Float
    ) : RoxyOperationResult
}
