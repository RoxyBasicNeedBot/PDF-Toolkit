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
        // 1. Camera Viewfinder with Adobe Scan-Style Crop Guide Quad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw detected document quadrilateral boundary (Adobe Scan #2175C8)
                val path = Path().apply {
                    moveTo(liveQuad.topLeft.x * w, liveQuad.topLeft.y * h)
                    lineTo(liveQuad.topRight.x * w, liveQuad.topRight.y * h)
                    lineTo(liveQuad.bottomRight.x * w, liveQuad.bottomRight.y * h)
                    lineTo(liveQuad.bottomLeft.x * w, liveQuad.bottomLeft.y * h)
                    close()
                }

                // Semi-transparent blue fill
                drawPath(
                    path = path,
                    color = RoxyColor.CropGuideFill
                )

                // High-precision guide line
                drawPath(
                    path = path,
                    color = RoxyColor.CropGuide,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 4 Interactive Magnifying Corner Handles (Adobe Scan Style)
                val outerRadius = 12.dp.toPx()
                val innerRadius = 5.dp.toPx()
                val corners = listOf(
                    Offset(liveQuad.topLeft.x * w, liveQuad.topLeft.y * h),
                    Offset(liveQuad.topRight.x * w, liveQuad.topRight.y * h),
                    Offset(liveQuad.bottomRight.x * w, liveQuad.bottomRight.y * h),
                    Offset(liveQuad.bottomLeft.x * w, liveQuad.bottomLeft.y * h)
                )

                for (corner in corners) {
                    drawCircle(Color.White, outerRadius, corner)
                    drawCircle(RoxyColor.CropGuide, outerRadius, corner, style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(RoxyColor.CropGuide, innerRadius, corner)
                }
            }
        }

        // 2. Top Coachmark Banner (Adobe Scan Style)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xCC0D1117))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (pagesCount == 0) "Looking for document..." else "$pagesCount ${if (pagesCount == 1) "Page" else "Pages"} Scanned",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }

        // 3. Bottom Controls Area (Mode Selector + Shutter Dock)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Document Mode Selector (Adobe Scan: Whiteboard, Book, Document, ID Card)
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Whiteboard",
                    color = RoxyColor.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Document",
                    color = RoxyColor.ScanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID Card",
                    color = RoxyColor.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Book",
                    color = RoxyColor.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Shutter Row (Gallery, Shutter Button, Page Review Thumbnail)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Photos / Gallery Icon
                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = CircleShape,
                    scrimAlpha = 0.35f,
                    onClick = onCaptureTrigger,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Import from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: Adobe Scan-Style Shutter Button (Dual-Ring Solid White + Blue Icon)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x331473E6))
                        .clickable(onClick = onCaptureTrigger),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Capture Document",
                            tint = RoxyColor.ScanAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Right: Scanned Pages Counter / Review Button
                if (pagesCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoxyColor.ScanAccent)
                            .clickable(onClick = onFinishScanning),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = "Review Scanned PDF",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$pagesCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar: Page Counter & Save PDF Button (Adobe Scan Top-Right Header)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pageIndex + 1} / $totalPages",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Delete Page Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33EF4444))
                        .clickable(onClick = onDeletePage),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Page",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Adobe Scan-Style "Save PDF" Action Button in Top Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(RoxyColor.ScanAccent)
                        .clickable(onClick = onSavePdf)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "Save PDF",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Save PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center: Warped & Cleaned Document Preview Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1520))
                .border(1.dp, Color(0x221473E6), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0x221473E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = "Magic Cleaned",
                        tint = RoxyColor.ScanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = page.filterType.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Luminance Normalized • Shadow Removed",
                    color = RoxyColor.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Switcher Dock (Adobe Scan: Magic Clean, Original, Grayscale, B&W)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141A26))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RoxyScanFilterType.entries.forEach { filter ->
                val isSelected = page.filterType == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RoxyColor.ScanAccent else Color.Transparent)
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.displayName,
                        color = if (isSelected) Color.White else RoxyColor.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Add Page & Save Document
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2638))
                    .clickable(onClick = onAddAnotherPage)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Add Page",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add Page",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoxyColor.ScanAccent)
                    .clickable(onClick = onSavePdf)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Save PDF",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Save PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
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
