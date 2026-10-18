package com.roxy.pdftoolkit.engine.scanner

/**
 * Enhancement filters for scanned documents (inspired by Adobe Scan's Magic Clean).
 */
enum class RoxyScanFilterType(val displayName: String) {
    /** Original photo with no color alterations */
    ORIGINAL("Original"),

    /** Magic Clean: Shadow removal, paper whitening, text contrast boost */
    MAGIC_CLEAN("Magic Clean"),

    /** Crisp Grayscale: Removes color noise, preserves smooth document gradients */
    GRAYSCALE("Grayscale"),

    /** High-Contrast Black & White: Pure binarized document for maximum text sharpness */
    BLACK_AND_WHITE("B & W")
}
