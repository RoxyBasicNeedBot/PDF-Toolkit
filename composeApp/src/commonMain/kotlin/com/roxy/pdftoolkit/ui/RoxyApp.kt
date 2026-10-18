package com.roxy.pdftoolkit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.roxy.pdftoolkit.ui.glass.rememberRoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.roxyBackdropSource
import com.roxy.pdftoolkit.ui.home.RoxyHomeScreen
import com.roxy.pdftoolkit.ui.navigation.RoxyGlassNavBar
import com.roxy.pdftoolkit.ui.navigation.RoxyNavDestination
import com.roxy.pdftoolkit.ui.theme.RoxyMeshBackground
import com.roxy.pdftoolkit.ui.theme.RoxyTheme
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeToolScreen
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import com.roxy.pdftoolkit.ui.tools.scan.RoxyScanToolScreen
import com.roxy.pdftoolkit.ui.tools.scan.RoxyScanViewModel
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

        // Inject ViewModels via Koin
        val mergeViewModel: RoxyMergeViewModel = koinViewModel()
        val scanViewModel: RoxyScanViewModel = koinViewModel()

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

                activeToolId == "scan" || currentNavDestination is RoxyNavDestination.Scan -> {
                    RoxyScanToolScreen(
                        viewModel = scanViewModel,
                        onBackClick = {
                            activeToolId = null
                            currentNavDestination = RoxyNavDestination.Tools
                        },
                        onSaveRequested = { onSaveFileRequest?.invoke() },
                        onOpenFile = { uri -> onOpenFileRequest?.invoke(uri) },
                        onShareFile = { uri -> onShareFileRequest?.invoke(uri) }
                    )
                }

                else -> {
                    // Main Tab Screens
                    when (currentNavDestination) {
                        is RoxyNavDestination.Tools -> {
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId ->
                                    RoxyCrashTracker.logAction("Tool clicked: $toolId", category = "ui")
                                    activeToolId = toolId
                                }
                            )
                        }
                        is RoxyNavDestination.Files -> {
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId ->
                                    RoxyCrashTracker.logAction("Tool clicked: $toolId", category = "ui")
                                    activeToolId = toolId
                                }
                            )
                        }
                        is RoxyNavDestination.Scan -> {
                            // Handled above in when branch
                        }
                        is RoxyNavDestination.Settings -> {
                            RoxyHomeScreen(
                                backdrop = backdrop,
                                onToolClick = { toolId ->
                                    RoxyCrashTracker.logAction("Tool clicked: $toolId", category = "ui")
                                    activeToolId = toolId
                                }
                            )
                        }
                    }
                }
            }

            // 3. Floating Bottom Navigation Bar (Visible only when not inside a full tool screen)
            AnimatedVisibility(
                visible = activeToolId == null && currentNavDestination !is RoxyNavDestination.Scan,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                RoxyGlassNavBar(
                    selectedDestination = currentNavDestination,
                    onDestinationSelected = { dest ->
                        RoxyCrashTracker.logAction("Nav destination: ${dest.title}", category = "navigation")
                        currentNavDestination = dest
                    },
                    backdrop = backdrop
                )
            }
        }
    }
}
