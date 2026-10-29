#include "roxy_magic_clean.h"

// Convert RGB to Luminance (Rec. 709)
static inline float getLuminance(uint8_t r, uint8_t g, uint8_t b) {
    return 0.2126f * r + 0.7152f * g + 0.0722f * b;
}

// Magic Clean: White Paper Whitening + Shadow Removal + Contrast Boost
// Modeled after Adobe Scan's CIELAB/Luminance normalization
void applyMagicCleanFilter(uint32_t* pixels, int width, int height) {
    int totalPixels = width * height;
    if (totalPixels <= 0) return;

    // Step 1: Sample luminance histogram to estimate paper white point and text dark point
    int hist[256] = {0};
    int sampleStep = std::max(1, totalPixels / 50000); // Sample ~50,000 pixels for speed

    for (int i = 0; i < totalPixels; i += sampleStep) {
        uint32_t p = pixels[i];
        uint8_t r = (p >> 16) & 0xFF;
        uint8_t g = (p >> 8) & 0xFF;
        uint8_t b = p & 0xFF;
        uint8_t lum = (uint8_t)std::clamp((int)std::round(getLuminance(r, g, b)), 0, 255);
        hist[lum]++;
    }

    // Find 90th percentile luminance (paper white reference) and 5th percentile (ink reference)
    int countTotal = 0;
    for (int i = 0; i < 256; i++) countTotal += hist[i];

    int target5 = (int)(countTotal * 0.05);
    int target90 = (int)(countTotal * 0.90);

    int blackPoint = 0;
    int whitePoint = 255;
    int accum = 0;

    for (int i = 0; i < 256; i++) {
        accum += hist[i];
        if (accum >= target5 && blackPoint == 0) {
            blackPoint = i;
        }
        if (accum >= target90) {
            whitePoint = i;
            break;
        }
    }

    blackPoint = std::clamp(blackPoint, 0, 80);
    whitePoint = std::clamp(whitePoint, 150, 255);
    float range = (float)(whitePoint - blackPoint);
    if (range < 10.0f) range = 10.0f;
    float invRange = 255.0f / range;

    // Step 2: Apply non-linear curve to push near-white backgrounds to pure #FFFFFF
    // while deepening text ink
    for (int i = 0; i < totalPixels; i++) {
        uint32_t p = pixels[i];
        uint32_t a = (p >> 24) & 0xFF;
        float r = (float)((p >> 16) & 0xFF);
        float g = (float)((p >> 8) & 0xFF);
        float b = (float)(p & 0xFF);

        // Normalized contrast stretch
        float nr = (r - blackPoint) * invRange;
        float ng = (g - blackPoint) * invRange;
        float nb = (b - blackPoint) * invRange;

        // Paper whitening boost: if luminance is above threshold, accelerate towards 255
        float lum = getLuminance(std::clamp((int)nr, 0, 255),
                                 std::clamp((int)ng, 0, 255),
                                 std::clamp((int)nb, 0, 255));

        if (lum > 180.0f) {
            float t = (lum - 180.0f) / 75.0f; // 0.0 to 1.0
            float boost = t * 30.0f;
            nr += boost;
            ng += boost;
            nb += boost;
        }

        uint32_t outR = (uint32_t)std::clamp((int)std::round(nr), 0, 255);
        uint32_t outG = (uint32_t)std::clamp((int)std::round(ng), 0, 255);
        uint32_t outB = (uint32_t)std::clamp((int)std::round(nb), 0, 255);

        pixels[i] = (a << 24) | (outR << 16) | (outG << 8) | outB;
    }
}

// Crisp Grayscale
void applyGrayscaleFilter(uint32_t* pixels, int width, int height) {
    int total = width * height;
    for (int i = 0; i < total; i++) {
        uint32_t p = pixels[i];
        uint32_t a = (p >> 24) & 0xFF;
        uint8_t r = (p >> 16) & 0xFF;
        uint8_t g = (p >> 8) & 0xFF;
        uint8_t b = p & 0xFF;

        uint8_t gray = (uint8_t)std::clamp((int)std::round(getLuminance(r, g, b)), 0, 255);
        pixels[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
    }
}

// High-Contrast Black & White (Otsu-based binarization)
void applyBinarizeFilter(uint32_t* pixels, int width, int height) {
    int total = width * height;
    if (total <= 0) return;

    // Otsu's thresholding algorithm
    int hist[256] = {0};
    for (int i = 0; i < total; i++) {
        uint32_t p = pixels[i];
        uint8_t lum = (uint8_t)getLuminance((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);
        hist[lum]++;
    }

    double sum = 0;
    for (int t = 0; t < 256; t++) sum += t * hist[t];

    double sumB = 0;
    int wB = 0;
    double varMax = 0;
    int threshold = 128;

    for (int t = 0; t < 256; t++) {
        wB += hist[t];
        if (wB == 0) continue;
        int wF = total - wB;
        if (wF == 0) break;

        sumB += (double)(t * hist[t]);
        double mB = sumB / wB;
        double mF = (sum - sumB) / wF;

        double varBetween = (double)wB * (double)wF * (mB - mF) * (mB - mF);
        if (varBetween > varMax) {
            varMax = varBetween;
            threshold = t;
        }
    }

    // Bias slightly higher to whiten paper background aggressively
    threshold = std::min(220, threshold + 20);

    for (int i = 0; i < total; i++) {
        uint32_t p = pixels[i];
        uint32_t a = (p >> 24) & 0xFF;
        uint8_t lum = (uint8_t)getLuminance((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);

        uint32_t val = (lum >= threshold) ? 0xFF : 0x00;
        pixels[i] = (a << 24) | (val << 16) | (val << 8) | val;
    }
}

// --- JNI Export Implementations ---

extern "C" JNIEXPORT jboolean JNICALL
Java_com_roxy_pdftoolkit_engine_scanner_RoxyNativeScannerBridge_nativeWarpAndFilter(
    JNIEnv* env,
    jobject /* thiz */,
    jobject inputBitmap,
    jfloatArray quadPoints,
    jobject outputBitmap,
    jint filterType
) {
    if (!inputBitmap || !outputBitmap || !quadPoints) {
        LOGE("nativeWarpAndFilter: null arguments provided");
        return JNI_FALSE;
    }

    AndroidBitmapInfo srcInfo, dstInfo;
    if (AndroidBitmap_getInfo(env, inputBitmap, &srcInfo) < 0 ||
        AndroidBitmap_getInfo(env, outputBitmap, &dstInfo) < 0) {
        LOGE("Failed to get bitmap info");
        return JNI_FALSE;
    }

    if (srcInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        dstInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Unsupported bitmap format (must be RGBA_8888)");
        return JNI_FALSE;
    }

    jfloat* points = env->GetFloatArrayElements(quadPoints, nullptr);
    if (!points || env->GetArrayLength(quadPoints) < 8) {
        LOGE("Invalid quad points array length");
        if (points) env->ReleaseFloatArrayElements(quadPoints, points, JNI_ABORT);
        return JNI_FALSE;
    }

    Quad2D quad;
    quad.tl = { points[0], points[1] };
    quad.tr = { points[2], points[3] };
    quad.br = { points[4], points[5] };
    quad.bl = { points[6], points[7] };
    env->ReleaseFloatArrayElements(quadPoints, points, JNI_ABORT);

    uint32_t* srcPixels = nullptr;
    uint32_t* dstPixels = nullptr;

    if (AndroidBitmap_lockPixels(env, inputBitmap, (void**)&srcPixels) < 0) {
        LOGE("Failed to lock source bitmap pixels");
        return JNI_FALSE;
    }

    if (AndroidBitmap_lockPixels(env, outputBitmap, (void**)&dstPixels) < 0) {
        AndroidBitmap_unlockPixels(env, inputBitmap);
        LOGE("Failed to lock destination bitmap pixels");
        return JNI_FALSE;
    }

    // 1. Perspective Homography Warp
    double H[3][3];
    computeHomography(quad, (float)dstInfo.width, (float)dstInfo.height, H);
    warpPerspectiveBilinear(srcPixels, srcInfo.width, srcInfo.height, dstPixels, dstInfo.width, dstInfo.height, H);

    // 2. Apply Enhancement Filter
    switch (filterType) {
        case FILTER_MAGIC_CLEAN:
            applyMagicCleanFilter(dstPixels, dstInfo.width, dstInfo.height);
            break;
        case FILTER_GRAYSCALE:
            applyGrayscaleFilter(dstPixels, dstInfo.width, dstInfo.height);
            break;
        case FILTER_BLACK_AND_WHITE:
            applyBinarizeFilter(dstPixels, dstInfo.width, dstInfo.height);
            break;
        case FILTER_ORIGINAL:
        default:
            break;
    }

    AndroidBitmap_unlockPixels(env, inputBitmap);
    AndroidBitmap_unlockPixels(env, outputBitmap);

    LOGI("nativeWarpAndFilter completed successfully (%dx%d -> %dx%d, filter=%d)",
         srcInfo.width, srcInfo.height, dstInfo.width, dstInfo.height, filterType);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_roxy_pdftoolkit_engine_scanner_RoxyNativeScannerBridge_nativeMagicClean(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap
) {
    if (!bitmap) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return JNI_FALSE;
    }

    uint32_t* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels) < 0) {
        return JNI_FALSE;
    }

    applyMagicCleanFilter(pixels, info.width, info.height);
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}
