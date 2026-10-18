package com.roxy.pdftoolkit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roxy.pdftoolkit.ui.glass.rememberRoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.roxyBackdropSource
import com.roxy.pdftoolkit.ui.home.RoxyHomeScreen
import com.roxy.pdftoolkit.ui.navigation.RoxyGlassNavBar
import com.roxy.pdftoolkit.ui.navigation.RoxyNavDestination
import com.roxy.pdftoolkit.ui.theme.RoxyMeshBackground
import com.roxy.pdftoolkit.ui.theme.RoxyTheme
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeToolScreen
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root Composable for Roxy PDF Toolkit.
 */
@Composable
fun RoxyApp(
    initialSharedUris: List<String> = emptyList(),
    onPickFilesRequest: (() -> Unit)? = null,
    onSaveFileRequest: (() -> Unit)? = null,
    onShareFileRequest: ((String) -> Unit)? = null,
    onOpenFileRequest: ((String) -> Unit)? = null
) {
    RoxyTheme {
        val backdrop = rememberRoxyBackdrop()
        var currentNavDestination by remember { mutableStateOf<RoxyNavDestination>(RoxyNavDestination.Tools) }
        var activeToolId by remember { mutableStateOf<String?>(null) }

        // Inject ViewModel
        val mergeViewModel: RoxyMergeViewModel = koinViewModel()

        // Handle incoming shared files (e.g. from WhatsApp intent filter)
        LaunchedEffect(initialSharedUris) {
            if (initialSharedUris.isNotEmpty()) {
                activeToolId = "merge"
                mergeViewModel.addDocuments(initialSharedUris)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .roxyBackdropSource(backdrop)
        ) {
            // 1. Animated background mesh
            RoxyMeshBackground()

            // 2. Active Screen Content
            when {
                activeToolId == "merge" -> {
                    RoxyMergeToolScreen(
                        viewModel = mergeViewModel,
                        onBackClick = { activeToolId = null },
                        onPickFilesClick = { onPickFilesRequest?.invoke() },
                        onRequestSaveDestination = { onSaveFileRequest?.invoke() },
                        onShareResultClick = { uri -> onShareFileRequest?.invoke(uri) },
                        onOpenResultClick = { uri -> onOpenFileRequest?.invoke(uri) }
                    )
                }

                else -> {
                    // Main Tab Screens
                    when (currentNavDestination) {
                        is RoxyNavDestination.Tools -> {
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId -> activeToolId = toolId }
                            )
                        }
                        is RoxyNavDestination.Files -> {
                            // Recent files hub (placeholder for Phase 1)
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId -> activeToolId = toolId }
                            )
                        }
                        is RoxyNavDestination.Scan -> {
                            // Document Scanner tab
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId -> activeToolId = toolId }
                            )
                        }
                        is RoxyNavDestination.Settings -> {
                            // Settings tab
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId -> activeToolId = toolId }
                            )
                        }
                    }
                }
            }

            // 3. Floating Bottom Navigation Bar (Visible only on top-level screens)
            AnimatedVisibility(
                visible = activeToolId == null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RoxyGlassNavBar(
                    selectedDestination = currentNavDestination,
                    onDestinationSelected = { currentNavDestination = it },
                    backdrop = backdrop
                )
            }
        }
    }
}
