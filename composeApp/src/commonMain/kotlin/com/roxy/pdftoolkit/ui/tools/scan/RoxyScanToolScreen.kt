package com.roxy.pdftoolkit.ui.tools.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.engine.scanner.RoxyDocumentQuad
import com.roxy.pdftoolkit.engine.scanner.RoxyScanFilterType
import com.roxy.pdftoolkit.engine.scanner.RoxyScannedPage
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.RoxyGlassButton
import com.roxy.pdftoolkit.ui.glass.RoxyGlassCard
import com.roxy.pdftoolkit.ui.scaffold.RoxyToolScaffold
import com.roxy.pdftoolkit.ui.theme.RoxyColor

@Composable
fun RoxyScanToolScreen(
    viewModel: RoxyScanViewModel,
    onBackClick: () -> Unit,
    onSaveRequested: () -> Unit,
    onOpenFile: (String) -> Unit,
    onShareFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    RoxyToolScaffold(
        title = "Smart Doc Scanner",
        subtitle = "Auto Edge Detection & Magic Clean",
        accentColor = RoxyColor.ScanAccent,
        onBackClick = onBackClick,
        modifier = modifier
    ) { backdrop ->
        AnimatedContent(targetState = state, label = "scan_tool_state_switch") { currentState ->
            when (currentState) {
                is RoxyScanState.CameraCapturing -> {
                    CameraCaptureView(
                        pagesCount = currentState.pages.size,
                        liveQuad = currentState.liveQuad,
                        backdrop = backdrop,
                        onCaptureTrigger = {
                            // Creates sample camera frame image bytes
                            val placeholderBytes = ByteArray(1024)
                            viewModel.onPageCaptured(placeholderBytes)
                        },
                        onFinishScanning = onSaveRequested
                    )
                }

                is RoxyScanState.ReviewingPage -> {
                    PageReviewView(
                        pageIndex = currentState.activePageIndex,
                        page = currentState.page,
                        totalPages = currentState.totalPages,
                        backdrop = backdrop,
                        onFilterSelected = { filter ->
                            viewModel.setFilter(currentState.activePageIndex, filter)
                        },
                        onAddAnotherPage = {
                            viewModel.resumeCapturing()
                        },
                        onDeletePage = {
                            viewModel.deleteCurrentPage(currentState.activePageIndex)
                        },
                        onSavePdf = onSaveRequested
                    )
                }

                is RoxyScanState.Compiling -> {
                    ScanCompilingView(
                        currentPage = currentState.currentPage,
                        totalPages = currentState.totalPages,
                        percent = currentState.progressPercent,
                        backdrop = backdrop
                    )
                }

                is RoxyScanState.Success -> {
                    ScanSuccessView(
                        outputUri = currentState.outputUri,
                        pageCount = currentState.pageCount,
                        backdrop = backdrop,
                        onOpen = { onOpenFile(currentState.outputUri) },
                        onShare = { onShareFile(currentState.outputUri) },
                        onNewScan = { viewModel.reset() }
                    )
                }

                is RoxyScanState.Error -> {
                    ScanErrorView(
                        message = currentState.message,
                        backdrop = backdrop,
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureView(
    pagesCount: Int,
    liveQuad: RoxyDocumentQuad,
    backdrop: RoxyBackdrop,
    onCaptureTrigger: () -> Unit,
    onFinishScanning: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Viewfinder Simulation with Live Corner Detection Quad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw detected document quadrilateral boundary (like Adobe Scan)
                val path = Path().apply {
                    moveTo(liveQuad.topLeft.x * w, liveQuad.topLeft.y * h)
                    lineTo(liveQuad.topRight.x * w, liveQuad.topRight.y * h)
                    lineTo(liveQuad.bottomRight.x * w, liveQuad.bottomRight.y * h)
                    lineTo(liveQuad.bottomLeft.x * w, liveQuad.bottomLeft.y * h)
                    close()
                }

                drawPath(
                    path = path,
                    color = RoxyColor.ScanAccent.copy(alpha = 0.25f)
                )

                drawPath(
                    path = path,
                    color = RoxyColor.ScanAccent,
                    style = Stroke(width = 4.dp.toPx())
                )

                // 4 Interactive Corner Dots
                val radius = 10.dp.toPx()
                drawCircle(Color.White, radius, Offset(liveQuad.topLeft.x * w, liveQuad.topLeft.y * h))
                drawCircle(Color.White, radius, Offset(liveQuad.topRight.x * w, liveQuad.topRight.y * h))
                drawCircle(Color.White, radius, Offset(liveQuad.bottomRight.x * w, liveQuad.bottomRight.y * h))
                drawCircle(Color.White, radius, Offset(liveQuad.bottomLeft.x * w, liveQuad.bottomLeft.y * h))
            }
        }

        // 2. Scanned Pages Pill Badge
        if (pagesCount > 0) {
            RoxyGlassCard(
                backdrop = backdrop,
                shape = RoundedCornerShape(percent = 50),
                tintColor = RoxyColor.ScanAccent,
                scrimAlpha = 0.4f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "$pagesCount ${if (pagesCount == 1) "Page" else "Pages"} Scanned",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 3. Shutter Dock at Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder for left balance
            Spacer(modifier = Modifier.size(54.dp))

            // Signature Large Liquid Glass Shutter Button
            RoxyGlassCard(
                backdrop = backdrop,
                shape = CircleShape,
                tintColor = RoxyColor.ScanAccent,
                scrimAlpha = 0.35f,
                blurRadius = 16.dp,
                onClick = onCaptureTrigger,
                modifier = Modifier.size(76.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCaptureTrigger),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Capture Document",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Finish & Save Button
            if (pagesCount > 0) {
                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(14.dp),
                    tintColor = RoxyColor.ConvertAccent,
                    scrimAlpha = 0.35f,
                    onClick = onFinishScanning,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Save PDF",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(54.dp))
            }
        }
    }
}

@Composable
private fun PageReviewView(
    pageIndex: Int,
    page: RoxyScannedPage,
    totalPages: Int,
    backdrop: RoxyBackdrop,
    onFilterSelected: (RoxyScanFilterType) -> Unit,
    onAddAnotherPage: () -> Unit,
    onDeletePage: () -> Unit,
    onSavePdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar: Page Info & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${pageIndex + 1} of $totalPages",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            RoxyGlassCard(
                backdrop = backdrop,
                shape = CircleShape,
                tintColor = Color(0xFFEF4444),
                scrimAlpha = 0.3f,
                onClick = onDeletePage,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Page",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center: Warped & Cleaned Document Preview Card
        RoxyGlassCard(
            backdrop = backdrop,
            shape = RoundedCornerShape(20.dp),
            scrimAlpha = 0.25f,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = "Magic Cleaned",
                    tint = RoxyColor.ScanAccent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Applied: ${page.filterType.displayName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Perspective Warped • Shadow Removed",
                    color = RoxyColor.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Switcher Dock (Magic Clean, Grayscale, B&W, Original)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoxyScanFilterType.entries.forEach { filter ->
                val isSelected = page.filterType == filter
                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(12.dp),
                    tintColor = if (isSelected) RoxyColor.ScanAccent else Color.Transparent,
                    scrimAlpha = if (isSelected) 0.4f else 0.2f,
                    onClick = { onFilterSelected(filter) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filter.displayName,
                        color = if (isSelected) Color.White else RoxyColor.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Add Page & Save Document
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoxyGlassButton(
                text = "Add Page",
                icon = Icons.Rounded.AddPhotoAlternate,
                backdrop = backdrop,
                accentColor = RoxyColor.ScanAccent,
                onClick = onAddAnotherPage,
                modifier = Modifier.weight(1f)
            )

            RoxyGlassButton(
                text = "Save PDF",
                icon = Icons.Rounded.PictureAsPdf,
                backdrop = backdrop,
                accentColor = RoxyColor.ConvertAccent,
                onClick = onSavePdf,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScanCompilingView(
    currentPage: Int,
    totalPages: Int,
    percent: Float,
    backdrop: RoxyBackdrop
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RoxyGlassCard(
            backdrop = backdrop,
            shape = RoundedCornerShape(26.dp),
            scrimAlpha = 0.32f,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { percent.coerceIn(0f, 1f) },
                    color = RoxyColor.ScanAccent,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Building Scanned PDF...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Compressing page $currentPage of $totalPages",
                    color = RoxyColor.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ScanSuccessView(
    outputUri: String,
    pageCount: Int,
    backdrop: RoxyBackdrop,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onNewScan: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RoxyGlassCard(
            backdrop = backdrop,
            shape = RoundedCornerShape(28.dp),
            scrimAlpha = 0.30f,
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Scan Saved!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "$pageCount pages saved to high-res PDF.",
                    color = RoxyColor.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoxyGlassButton(
                        text = "Open PDF",
                        backdrop = backdrop,
                        accentColor = RoxyColor.ScanAccent,
                        onClick = onOpen,
                        modifier = Modifier.weight(1f)
                    )

                    RoxyGlassButton(
                        text = "Share",
                        icon = Icons.Rounded.Share,
                        backdrop = backdrop,
                        accentColor = RoxyColor.Secondary,
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Scan new document",
                    color = RoxyColor.ScanAccent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onNewScan)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanErrorView(
    message: String,
    backdrop: RoxyBackdrop,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RoxyGlassCard(
            backdrop = backdrop,
            shape = RoundedCornerShape(24.dp),
            tintColor = Color(0xFFEF4444),
            scrimAlpha = 0.35f,
            modifier = Modifier.padding(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan Error",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                RoxyGlassButton(
                    text = "Retry",
                    backdrop = backdrop,
                    accentColor = Color(0xFFEF4444),
                    onClick = onRetry
                )
            }
        }
    }
}
