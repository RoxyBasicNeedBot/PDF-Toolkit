package com.roxy.pdftoolkit.engine.scanner

import android.graphics.Bitmap
import android.util.Log

/**
 * High-performance Native JNI Bridge for Document Warping & Magic Clean Filters.
 * Modeled after Adobe Scan's CameraCleanAndroidShim and libMagicClean.so.
 */
object RoxyNativeScannerBridge {

    private const val TAG = "RoxyNativeBridge"

    val isNativeLoaded: Boolean = try {
        System.loadLibrary("roxy_magic_clean")
        Log.i(TAG, "libroxy_magic_clean.so loaded successfully!")
        true
    } catch (e: Throwable) {
        Log.w(TAG, "Native C++ library not available, fallback will be used: ${e.message}")
        false
    }

    /**
     * Native C++ 4-point perspective warp and image filter.
     */
    external fun nativeWarpAndFilter(
        inputBitmap: Bitmap,
        quadPoints: FloatArray,
        outputBitmap: Bitmap,
        filterType: Int
    ): Boolean

    /**
     * Native C++ Magic Clean (Luminance normalization & shadow removal).
     */
    external fun nativeMagicClean(
        bitmap: Bitmap
    ): Boolean

    /**
     * High-level safe invocation with seamless fallback.
     */
    fun warpAndFilter(
        inputBitmap: Bitmap,
        quad: RoxyDocumentQuad,
        outputBitmap: Bitmap,
        filterType: RoxyScanFilterType
    ): Boolean {
        if (!isNativeLoaded) return false

        return try {
            val filterCode = when (filterType) {
                RoxyScanFilterType.ORIGINAL -> 0
                RoxyScanFilterType.MAGIC_CLEAN -> 1
                RoxyScanFilterType.GRAYSCALE -> 2
                RoxyScanFilterType.BLACK_AND_WHITE -> 3
            }

            // Un-normalize quad points to inputBitmap dimensions
            val w = inputBitmap.width.toFloat()
            val h = inputBitmap.height.toFloat()

            val points = floatArrayOf(
                quad.topLeft.x * w, quad.topLeft.y * h,
                quad.topRight.x * w, quad.topRight.y * h,
                quad.bottomRight.x * w, quad.bottomRight.y * h,
                quad.bottomLeft.x * w, quad.bottomLeft.y * h
            )

            nativeWarpAndFilter(inputBitmap, points, outputBitmap, filterCode)
        } catch (e: Throwable) {
            Log.e(TAG, "Native warpAndFilter failed, falling back to Canvas: ${e.message}")
            false
        }
    }
}
