package com.roxy.pdftoolkit.tier

import android.os.Build
import com.roxy.pdftoolkit.ui.glass.RoxyDeviceTier

object RoxyDeviceTierDetector {
    /**
     * Detects device capability based on Android OS version.
     * Android 13+ supports AGSL RuntimeShader (lens refraction & chromatic aberration).
     * Android 12 supports RenderEffect blur.
     * Android 11 and below uses hardware-accelerated translucent fallback.
     */
    fun detectDeviceTier(): RoxyDeviceTier {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                RoxyDeviceTier.TIER_3_FULL_AGSL
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                RoxyDeviceTier.TIER_2_BLUR_ONLY
            }
            else -> {
                RoxyDeviceTier.TIER_1_FALLBACK_TRANSLUCENT
            }
        }
    }
}
