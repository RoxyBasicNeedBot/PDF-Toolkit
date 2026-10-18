package com.roxy.pdftoolkit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinations accessible from the Roxy Floating Glass Nav Bar.
 */
sealed class RoxyNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Tools : RoxyNavDestination("tools", "Tools", Icons.Rounded.GridView)
    data object Files : RoxyNavDestination("files", "Files", Icons.Rounded.Description)
    data object Scan : RoxyNavDestination("scan", "Scan", Icons.Rounded.DocumentScanner)
    data object Settings : RoxyNavDestination("settings", "Settings", Icons.Rounded.Settings)

    companion object {
        val items: List<RoxyNavDestination>
            get() = listOf(Tools, Files, Scan, Settings)

        fun all(): List<RoxyNavDestination> = listOf(Tools, Files, Scan, Settings)
    }
}
