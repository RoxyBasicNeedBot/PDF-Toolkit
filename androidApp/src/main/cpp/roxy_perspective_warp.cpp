#include "roxy_magic_clean.h"

// Invert a 3x3 matrix
static bool invert3x3(const double A[3][3], double inv[3][3]) {
    double det = A[0][0] * (A[1][1] * A[2][2] - A[1][2] * A[2][1]) -
                 A[0][1] * (A[1][0] * A[2][2] - A[1][2] * A[2][0]) +
                 A[0][2] * (A[1][0] * A[2][1] - A[1][1] * A[2][0]);

    if (std::abs(det) < 1e-12) return false;
    double invDet = 1.0 / det;

    inv[0][0] =  (A[1][1] * A[2][2] - A[1][2] * A[2][1]) * invDet;
    inv[0][1] = -(A[0][1] * A[2][2] - A[0][2] * A[2][1]) * invDet;
    inv[0][2] =  (A[0][1] * A[1][2] - A[0][2] * A[1][1]) * invDet;

    inv[1][0] = -(A[1][0] * A[2][2] - A[1][2] * A[2][0]) * invDet;
    inv[1][1] =  (A[0][0] * A[2][2] - A[0][2] * A[2][0]) * invDet;
    inv[1][2] = -(A[0][0] * A[1][2] - A[0][2] * A[1][0]) * invDet;

    inv[2][0] =  (A[1][0] * A[2][1] - A[1][1] * A[2][0]) * invDet;
    inv[2][1] = -(A[0][0] * A[2][1] - A[0][1] * A[2][0]) * invDet;
    inv[2][2] =  (A[0][0] * A[1][1] - A[0][1] * A[1][0]) * invDet;

    return true;
}

// Compute 3x3 Homography from destination rectangle (0,0)->(W,H) to source quadrilateral
// This directly yields the mapping from (dst_x, dst_y) -> (src_x, src_y)
void computeHomography(const Quad2D& src, float dstW, float dstH, double H[3][3]) {
    // Mapping from unit square (0,0),(1,0),(1,1),(0,1) to quadrilateral src
    double x0 = src.tl.x, y0 = src.tl.y;
    double x1 = src.tr.x, y1 = src.tr.y;
    double x2 = src.br.x, y2 = src.br.y;
    double x3 = src.bl.x, y3 = src.bl.y;

    double dx1 = x1 - x2;
    double dx2 = x3 - x2;
    double sx  = x0 - x1 + x2 - x3;
    double dy1 = y1 - y2;
    double dy2 = y3 - y2;
    double sy  = y0 - y1 + y2 - y3;

    double h[3][3];

    if (std::abs(sx) < 1e-8 && std::abs(sy) < 1e-8) {
        // Affine mapping
        h[0][0] = x1 - x0;
        h[0][1] = x2 - x1;
        h[0][2] = x0;
        h[1][0] = y1 - y0;
        h[1][1] = y2 - y1;
        h[1][2] = y0;
        h[2][0] = 0.0;
        h[2][1] = 0.0;
        h[2][2] = 1.0;
    } else {
        // Projective mapping
        double d = dx1 * dy2 - dx2 * dy1;
        double a13 = (d != 0.0) ? (sx * dy2 - sy * dx2) / d : 0.0;
        double a23 = (d != 0.0) ? (dx1 * sy - dy1 * sx) / d : 0.0;

        h[0][0] = x1 - x0 + a13 * x1;
        h[0][1] = x3 - x0 + a23 * x3;
        h[0][2] = x0;
        h[1][0] = y1 - y0 + a13 * y1;
        h[1][1] = y3 - y0 + a23 * y3;
        h[1][2] = y0;
        h[2][0] = a13;
        h[2][1] = a23;
        h[2][2] = 1.0;
    }

    // Now compose with scaling (divide dst coordinates by dstW, dstH to get [0,1] normalized)
    H[0][0] = h[0][0] / dstW;
    H[0][1] = h[0][1] / dstH;
    H[0][2] = h[0][2];
    H[1][0] = h[1][0] / dstW;
    H[1][1] = h[1][1] / dstH;
    H[1][2] = h[1][2];
    H[2][0] = h[2][0] / dstW;
    H[2][1] = h[2][1] / dstH;
    H[2][2] = h[2][2];
}

// Bilinear interpolation sampling from source image
static inline uint32_t sampleBilinear(const uint32_t* src, int w, int h, float x, float y) {
    if (x < 0.0f || x >= w - 1 || y < 0.0f || y >= h - 1) {
        int cx = std::clamp((int)std::round(x), 0, w - 1);
        int cy = std::clamp((int)std::round(y), 0, h - 1);
        return src[cy * w + cx];
    }

    int x0 = (int)x;
    int y0 = (int)y;
    int x1 = x0 + 1;
    int y1 = y0 + 1;

    float fx = x - x0;
    float fy = y - y0;
    float ifx = 1.0f - fx;
    float ify = 1.0f - fy;

    uint32_t p00 = src[y0 * w + x0];
    uint32_t p10 = src[y0 * w + x1];
    uint32_t p01 = src[y1 * w + x0];
    uint32_t p11 = src[y1 * w + x1];

    // Unpack ARGB
    auto unpackA = [](uint32_t p) { return (p >> 24) & 0xFF; };
    auto unpackR = [](uint32_t p) { return (p >> 16) & 0xFF; };
    auto unpackG = [](uint32_t p) { return (p >> 8) & 0xFF; };
    auto unpackB = [](uint32_t p) { return p & 0xFF; };

    float r = ify * (ifx * unpackR(p00) + fx * unpackR(p10)) + fy * (ifx * unpackR(p01) + fx * unpackR(p11));
    float g = ify * (ifx * unpackG(p00) + fx * unpackG(p10)) + fy * (ifx * unpackG(p01) + fx * unpackG(p11));
    float b = ify * (ifx * unpackB(p00) + fx * unpackB(p10)) + fy * (ifx * unpackB(p01) + fx * unpackB(p11));
    float a = ify * (ifx * unpackA(p00) + fx * unpackA(p10)) + fy * (ifx * unpackA(p01) + fx * unpackA(p11));

    uint32_t ur = (uint32_t)std::clamp((int)std::round(r), 0, 255);
    uint32_t ug = (uint32_t)std::clamp((int)std::round(g), 0, 255);
    uint32_t ub = (uint32_t)std::clamp((int)std::round(b), 0, 255);
    uint32_t ua = (uint32_t)std::clamp((int)std::round(a), 0, 255);

    return (ua << 24) | (ur << 16) | (ug << 8) | ub;
}

void warpPerspectiveBilinear(
    const uint32_t* srcPixels, int srcW, int srcH,
    uint32_t* dstPixels, int dstW, int dstH,
    const double H_map[3][3]
) {
    for (int y = 0; y < dstH; y++) {
        uint32_t* row = dstPixels + y * dstW;
        double dy = (double)y;

        for (int x = 0; x < dstW; x++) {
            double dx = (double)x;

            // Project dst (x,y) -> src (u,v)
            double denom = H_map[2][0] * dx + H_map[2][1] * dy + H_map[2][2];
            if (std::abs(denom) > 1e-12) {
                float invDenom = (float)(1.0 / denom);
                float u = (float)(H_map[0][0] * dx + H_map[0][1] * dy + H_map[0][2]) * invDenom;
                float v = (float)(H_map[1][0] * dx + H_map[1][1] * dy + H_map[1][2]) * invDenom;

                row[x] = sampleBilinear(srcPixels, srcW, srcH, u, v);
            } else {
                row[x] = 0xFFFFFFFF; // White fallback
            }
        }
    }
}
