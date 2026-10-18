package com.roxy.pdftoolkit.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as nativeLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.roxy.pdftoolkit.ui.theme.LocalRoxyDeviceTier
import com.roxy.pdftoolkit.ui.theme.LocalRoxyIsDarkTheme
import com.roxy.pdftoolkit.ui.theme.RoxyColor

typealias RoxyBackdrop = LayerBackdrop

/**
 * Remembers a shared background layer that sibling Roxy Liquid Glass surfaces sample from.
 */
@Composable
fun rememberRoxyBackdrop(backgroundColor: Color = RoxyColor.BackgroundDark): RoxyBackdrop =
    rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

/**
 * Marks a composable as the backdrop source layer.
 */
fun Modifier.roxyBackdropSource(backdrop: RoxyBackdrop): Modifier =
    this.nativeLayerBackdrop(backdrop)

/**
 * Signature Liquid Glass modifier for Roxy PDF Toolkit.
 *
 * Implements real-time refraction optics, lens curvature, chromatic aberration,
 * and touch deformation scaled across 3 device performance tiers.
 */
@Composable
fun Modifier.roxyLiquidGlass(
    backdrop: RoxyBackdrop,
    shape: Shape = RoundedCornerShape(24.dp),
    interaction: RoxyGlassInteraction? = null,
    blurRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    scrimAlpha: Float = 0.28f,
    tintColor: Color? = null
): Modifier {
    val tier = LocalRoxyDeviceTier.current
    val isDark = LocalRoxyIsDarkTheme.current

    // Fallback Tier 1: Low-end devices or battery saver mode
    if (tier == RoxyDeviceTier.TIER_1_FALLBACK_TRANSLUCENT) {
        val fallbackBg = if (isDark) {
            Color(0xCC111827)
        } else {
            Color(0xEEFFFFFF)
        }
        return this
            .clip(shape)
            .background(tintColor?.copy(alpha = 0.25f) ?: fallbackBg)
            .border(borderWidth, RoxyColor.GlassWhiteRim.copy(alpha = 0.15f), shape)
    }

    // Pointer event recording for interactive touch glow
    val pointerModifier = if (interaction != null) {
        Modifier.pointerInput(interaction) {
            interaction.processPointer(this)
        }
    } else {
        Modifier
    }

    // Tier 2 & Tier 3: GPU Shader Backdrops
    return this
        .then(pointerModifier)
        .graphicsLayer {
            if (interaction != null && interaction.pressProgress > 0f) {
                val scale = lerp(1f, 1.04f, interaction.pressProgress)
                scaleX = scale
                scaleY = scale
            }
        }
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = { Highlight.Default.copy(alpha = 0.5f) },
            shadow = { Shadow(radius = 10.dp, alpha = 0.35f) },
            innerShadow = {
                val press = interaction?.pressProgress ?: 0f
                InnerShadow(
                    radius = lerpDp(6.dp, 12.dp, press),
                    alpha = lerp(0.3f, 0.6f, press)
                )
            },
            effects = {
                vibrancy()
                colorControls(
                    brightness = 0.04f,
                    contrast = 1.02f,
                    saturation = 1.4f
                )
                blur(blurRadius.toPx())

                // Tier 3 only: Real-time optical lens refraction & chromatic aberration
                if (tier == RoxyDeviceTier.TIER_3_FULL_AGSL) {
                    val press = interaction?.pressProgress ?: 0f
                    lens(
                        refractionHeight = 20.dp.toPx() + 6.dp.toPx() * press,
                        refractionAmount = size.minDimension / 2f,
                        chromaticAberration = true
                    )
                }
            },
            onDrawSurface = {
                // Luminance-protective scrim to prevent glass wash-out
                val baseDarken = if (isDark) Color.Black else Color.White
                drawRect(baseDarken.copy(alpha = scrimAlpha))

                if (tintColor != null) {
                    drawRect(tintColor.copy(alpha = 0.15f))
                }

                // Interactive radial glow follows user's finger on tap/drag
                val press = interaction?.pressProgress ?: 0f
                if (press > 0f && interaction != null) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f * press),
                                Color.Transparent
                            ),
                            center = interaction.touchPosition,
                            radius = size.minDimension * 1.4f
                        ),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        )
}
