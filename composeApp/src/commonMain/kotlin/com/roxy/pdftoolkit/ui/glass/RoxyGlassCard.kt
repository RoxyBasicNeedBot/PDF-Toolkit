package com.roxy.pdftoolkit.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Liquid Glass card container.
 */
@Composable
fun RoxyGlassCard(
    backdrop: RoxyBackdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    interactive: Boolean = true,
    tintColor: Color? = null,
    scrimAlpha: Float = 0.28f,
    blurRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = if (interactive) rememberRoxyGlassInteraction() else null

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .roxyLiquidGlass(
                backdrop = backdrop,
                shape = shape,
                interaction = interaction,
                blurRadius = blurRadius,
                scrimAlpha = scrimAlpha,
                tintColor = tintColor
            )
            .then(clickModifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}
