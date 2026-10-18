package com.roxy.pdftoolkit.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.rememberRoxyGlassInteraction
import com.roxy.pdftoolkit.ui.glass.roxyLiquidGlass
import com.roxy.pdftoolkit.ui.theme.RoxyColor

private val RoxyCapsuleShape = RoundedCornerShape(percent = 50)
private val NavBarHeight = 66.dp
private val BlobHeight = 54.dp
private val TabWidth = 76.dp

/**
 * Signature Floating Liquid Glass Dock for Roxy PDF Toolkit.
 *
 * Implements a 3-layer architecture:
 * 1. Base capsule in Liquid Glass with lens refraction and specular rim highlight.
 * 2. Sliding frosted pill that glides smoothly behind the active destination with spring physics.
 * 3. Crisp icon and typography labels on top.
 */
@Composable
fun RoxyGlassNavBar(
    selectedDestination: RoxyNavDestination,
    onDestinationSelected: (RoxyNavDestination) -> Unit,
    backdrop: RoxyBackdrop,
    modifier: Modifier = Modifier,
    destinations: List<RoxyNavDestination> = RoxyNavDestination.items
) {
    val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)
    val interaction = rememberRoxyGlassInteraction()

    // Smooth spring animation for sliding indicator
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "roxy_nav_slide"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .height(NavBarHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer Capsule Container
        Box(
            modifier = Modifier
                .height(NavBarHeight)
                .width(TabWidth * destinations.size + 16.dp)
                .roxyLiquidGlass(
                    backdrop = backdrop,
                    shape = RoxyCapsuleShape,
                    interaction = interaction,
                    blurRadius = 20.dp,
                    scrimAlpha = 0.32f
                )
                .clip(RoxyCapsuleShape),
            contentAlignment = Alignment.CenterStart
        ) {
            // Layer 2: Sliding Frosted Blob Indicator
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = (8.dp + TabWidth * animatedIndex).toPx()
                    }
                    .size(width = TabWidth, height = BlobHeight)
                    .roxyLiquidGlass(
                        backdrop = backdrop,
                        shape = RoxyCapsuleShape,
                        tintColor = RoxyColor.Primary,
                        scrimAlpha = 0.40f,
                        blurRadius = 12.dp
                    )
            )

            // Layer 3: Interactive Tabs (Icons & Text)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                destinations.forEachIndexed { index, destination ->
                    val isSelected = index == selectedIndex

                    Column(
                        modifier = Modifier
                            .width(TabWidth)
                            .height(BlobHeight)
                            .clip(RoxyCapsuleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onDestinationSelected(destination) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = if (isSelected) Color.White else RoxyColor.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = destination.title,
                            color = if (isSelected) Color.White else RoxyColor.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
