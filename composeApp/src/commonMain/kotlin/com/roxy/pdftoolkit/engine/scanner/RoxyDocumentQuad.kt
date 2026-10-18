package com.roxy.pdftoolkit.engine.scanner

data class RoxyPoint(val x: Float, val y: Float)

/**
 * 4-Corner Quadrilateral representing the detected document boundary.
 * Coordinates are normalized in [0.0f, 1.0f] relative to image dimensions.
 */
data class RoxyDocumentQuad(
    val topLeft: RoxyPoint = RoxyPoint(0.05f, 0.05f),
    val topRight: RoxyPoint = RoxyPoint(0.95f, 0.05f),
    val bottomRight: RoxyPoint = RoxyPoint(0.95f, 0.95f),
    val bottomLeft: RoxyPoint = RoxyPoint(0.05f, 0.95f)
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )

    companion object {
        val Default = RoxyDocumentQuad()
    }
}
