package com.roxy.pdftoolkit.engine.scanner

/**
 * An individual page within a scanning session.
 */
data class RoxyScannedPage(
    val id: String,
    val originalImageBytes: ByteArray,
    val quad: RoxyDocumentQuad = RoxyDocumentQuad.Default,
    val filterType: RoxyScanFilterType = RoxyScanFilterType.MAGIC_CLEAN,
    val processedImageBytes: ByteArray? = null,
    val rotationDegrees: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RoxyScannedPage
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
