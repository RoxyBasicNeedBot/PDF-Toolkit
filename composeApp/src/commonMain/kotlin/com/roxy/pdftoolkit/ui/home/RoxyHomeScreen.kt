package com.roxy.pdftoolkit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.RoxyGlassCard
import com.roxy.pdftoolkit.ui.theme.RoxyColor

data class RoxyToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun RoxyHomeScreen(
    backdrop: RoxyBackdrop,
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val organizeTools = listOf(
        RoxyToolItem("merge", "Merge PDF", "Combine multiple PDFs into one", Icons.Rounded.Merge, RoxyColor.OrganizeAccent),
        RoxyToolItem("split", "Split PDF", "Extract pages or separate ranges", Icons.Rounded.CallSplit, RoxyColor.OrganizeAccent),
        RoxyToolItem("organize", "Page Grid", "Reorder, rotate & delete pages", Icons.Rounded.SwapVert, RoxyColor.OrganizeAccent),
    )

    val convertTools = listOf(
        RoxyToolItem("img_to_pdf", "Images to PDF", "Turn photos into documents", Icons.Rounded.Image, RoxyColor.ConvertAccent),
        RoxyToolItem("pdf_to_img", "PDF to Images", "Export pages as high-res images", Icons.Rounded.Collections, RoxyColor.ConvertAccent),
    )

    val securityTools = listOf(
        RoxyToolItem("encrypt", "Protect PDF", "Encrypt with strong password", Icons.Rounded.Lock, RoxyColor.SecurityAccent),
        RoxyToolItem("decrypt", "Unlock PDF", "Remove document passwords", Icons.Rounded.LockOpen, RoxyColor.SecurityAccent),
    )

    val scanTools = listOf(
        RoxyToolItem("scan", "Doc Scanner", "Scan paper docs with edge detect", Icons.Rounded.DocumentScanner, RoxyColor.ScanAccent),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Roxy PDF",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "100% Offline & Private Toolkit",
                        color = RoxyColor.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(14.dp),
                    tintColor = RoxyColor.Primary,
                    scrimAlpha = 0.3f,
                    interactive = false,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Offline Protected",
                        tint = RoxyColor.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Organize
        item {
            SectionHeader("Organize & Edit")
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            ToolGrid(tools = organizeTools, backdrop = backdrop, onToolClick = onToolClick)
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section: Convert
        item {
            SectionHeader("Convert")
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            ToolGrid(tools = convertTools, backdrop = backdrop, onToolClick = onToolClick)
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section: Security
        item {
            SectionHeader("Security & Protection")
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            ToolGrid(tools = securityTools, backdrop = backdrop, onToolClick = onToolClick)
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Section: Scan
        item {
            SectionHeader("Smart Scanner")
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            ToolGrid(tools = scanTools, backdrop = backdrop, onToolClick = onToolClick)
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom glass dock
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = RoxyColor.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ToolGrid(
    tools: List<RoxyToolItem>,
    backdrop: RoxyBackdrop,
    onToolClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.chunked(2).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTools.forEach { tool ->
                    RoxyToolCard(
                        title = tool.title,
                        description = tool.description,
                        icon = tool.icon,
                        accentColor = tool.accentColor,
                        backdrop = backdrop,
                        onClick = { onToolClick(tool.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTools.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
