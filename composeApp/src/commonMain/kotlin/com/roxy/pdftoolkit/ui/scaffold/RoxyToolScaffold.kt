package com.roxy.pdftoolkit.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.rememberRoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.roxyBackdropSource
import com.roxy.pdftoolkit.ui.theme.RoxyColor
import com.roxy.pdftoolkit.ui.theme.RoxyMeshBackground

/**
 * Universal Scaffold for all Roxy PDF Tools.
 * Standardizes the 4-phase tool experience across the app.
 */
@Composable
fun RoxyToolScaffold(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = RoxyColor.Primary,
    bottomBar: @Composable (RoxyBackdrop) -> Unit = {},
    content: @Composable (RoxyBackdrop) -> Unit
) {
    val backdrop = rememberRoxyBackdrop()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Organic Mesh Background Tinted with Tool's Accent Color (Isolated backdrop source layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .roxyBackdropSource(backdrop)
        ) {
            RoxyMeshBackground(accentColor = accentColor)
        }

        // 2. Main Content & Header
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            RoxyToolHeader(
                title = title,
                subtitle = subtitle,
                backdrop = backdrop,
                onBackClick = onBackClick
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(backdrop)
            }
        }

        // 3. Floating Bottom Action Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            bottomBar(backdrop)
        }
    }
}
