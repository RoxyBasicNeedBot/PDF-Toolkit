package com.roxy.pdftoolkit.engine.model

/**
 * Metadata representation of a PDF file within Roxy PDF Toolkit.
 */
data class RoxyPdfDocument(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val isEncrypted: Boolean = false,
    val thumbnailBytes: ByteArray? = null
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> "${(mb * 10).toLong() / 10.0} MB"
                kb >= 1.0 -> "${(kb * 10).toLong() / 10.0} KB"
                else -> "$sizeBytes B"
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RoxyPdfDocument
        return uri == other.uri
    }

    override fun hashCode(): Int = uri.hashCode()
}
