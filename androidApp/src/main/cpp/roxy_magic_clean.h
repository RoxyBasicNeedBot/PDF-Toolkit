#ifndef ROXY_MAGIC_CLEAN_H
#define ROXY_MAGIC_CLEAN_H

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <cstdint>
#include <cmath>
#include <algorithm>

#define LOG_TAG "RoxyMagicCleanNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

enum RoxyFilterMode {
    FILTER_ORIGINAL = 0,
    FILTER_MAGIC_CLEAN = 1,
    FILTER_GRAYSCALE = 2,
    FILTER_BLACK_AND_WHITE = 3
};

// 4-point quadrilateral structure
struct Point2D {
    float x;
    float y;
};

struct Quad2D {
    Point2D tl;
    Point2D tr;
    Point2D br;
    Point2D bl;
};

// Transform & Filter functions
void computeHomography(const Quad2D& src, float dstW, float dstH, double H[3][3]);
void warpPerspectiveBilinear(
    const uint32_t* srcPixels, int srcW, int srcH,
    uint32_t* dstPixels, int dstW, int dstH,
    const double H_inv[3][3]
);
void applyMagicCleanFilter(uint32_t* pixels, int width, int height);
void applyGrayscaleFilter(uint32_t* pixels, int width, int height);
void applyBinarizeFilter(uint32_t* pixels, int width, int height);

#endif // ROXY_MAGIC_CLEAN_H
