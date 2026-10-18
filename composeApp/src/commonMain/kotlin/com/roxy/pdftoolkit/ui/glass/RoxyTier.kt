package com.roxy.pdftoolkit.ui.glass

/**
 * Hardware capability tiers for Roxy Liquid Glass rendering.
 * Ensures smooth 60fps on all Android devices, from budget phones to flagship flagships.
 */
enum class RoxyDeviceTier {
    /**
     * Tier 3: Android 13+ (API 33+) or Desktop with Skiko RuntimeShader.
     * Full optical glass simulation:
     * - AGSL Lens refraction
     * - Chromatic aberration (RGB channel splitting)
     * - Dynamic specular rim light highlight
     * - Pointer-tracking touch glow
     */
    TIER_3_FULL_AGSL,

    /**
     * Tier 2: Android 12 (API 31-32).
     * Hardware RenderEffect blur + vibrancy tint + rim light border.
     */
    TIER_2_BLUR_ONLY,

    /**
     * Tier 1: Android 11 or lower (API < 31), or when user toggles "Battery Saver / Reduced Effects".
     * Translucent flat surface with subtle border, 0% shader overhead.
     */
    TIER_1_FALLBACK_TRANSLUCENT
}
