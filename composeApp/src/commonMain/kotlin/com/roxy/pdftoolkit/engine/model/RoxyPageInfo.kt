package com.roxy.pdftoolkit.engine.model

/**
 * Information regarding an individual page in a PDF.
 * Used for thumbnail rendering, reordering, rotating, and splitting.
 */
data class RoxyPageInfo(
    val parentDocumentUri: String,
    val pageIndex: Int,
    val rotationDegrees: Int = 0,
    val thumbnailBytes: ByteArray? = null,
    val isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RoxyPageInfo
        return pageIndex == other.pageIndex && parentDocumentUri == other.parentDocumentUri
    }

    override fun hashCode(): Int {
        var result = parentDocumentUri.hashCode()
        result = 31 * result + pageIndex
        return result
    }
}
