package com.roxy.pdftoolkit.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Organic animated background gradient mesh.
 * Provides the rich vibrant backdrop necessary for Roxy Liquid Glass
 * refraction, lens warping, and chromatic aberration to pop.
 */
@Composable
fun RoxyMeshBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = RoxyColor.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "roxy_mesh_transition")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roxy_mesh_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Deep solid background base
        drawRect(color = RoxyColor.BackgroundDark)

        // 2. Animated floating primary glowing orb
        val orb1Center = Offset(
            x = width * (0.3f + 0.2f * sin(animationProgress)),
            y = height * (0.25f + 0.15f * cos(animationProgress))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                center = orb1Center,
                radius = width * 0.75f
            ),
            center = orb1Center,
            radius = width * 0.75f
        )

        // 3. Floating secondary Cyan orb
        val orb2Center = Offset(
            x = width * (0.7f - 0.2f * cos(animationProgress)),
            y = height * (0.6f + 0.2f * sin(animationProgress))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RoxyColor.Secondary.copy(alpha = 0.25f), Color.Transparent),
                center = orb2Center,
                radius = width * 0.65f
            ),
            center = orb2Center,
            radius = width * 0.65f
        )

        // 4. Subtle purple orb towards bottom
        val orb3Center = Offset(
            x = width * 0.5f,
            y = height * (0.85f + 0.1f * sin(animationProgress * 0.5f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RoxyColor.Tertiary.copy(alpha = 0.20f), Color.Transparent),
                center = orb3Center,
                radius = width * 0.8f
            ),
            center = orb3Center,
            radius = width * 0.8f
        )
    }
}
