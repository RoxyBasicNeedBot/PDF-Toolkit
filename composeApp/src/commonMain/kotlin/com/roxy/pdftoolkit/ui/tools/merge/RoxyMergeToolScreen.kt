package com.roxy.pdftoolkit.ui.tools.merge

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.engine.model.RoxyPdfDocument
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.RoxyGlassButton
import com.roxy.pdftoolkit.ui.glass.RoxyGlassCard
import com.roxy.pdftoolkit.ui.scaffold.RoxyToolScaffold
import com.roxy.pdftoolkit.ui.theme.RoxyColor

@Composable
fun RoxyMergeToolScreen(
    viewModel: RoxyMergeViewModel,
    onBackClick: () -> Unit,
    onPickFilesClick: () -> Unit,
    onRequestSaveDestination: () -> Unit,
    onShareResultClick: (String) -> Unit,
    onOpenResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    RoxyToolScaffold(
        title = "Merge PDF",
        subtitle = "Combine multiple PDFs in custom order",
        onBackClick = onBackClick,
        accentColor = RoxyColor.OrganizeAccent,
        modifier = modifier,
        bottomBar = { backdrop ->
            when (val s = state) {
                is RoxyMergeState.FilesSelected -> {
                    RoxyGlassButton(
                        text = "Merge ${s.files.size} Documents",
                        backdrop = backdrop,
                        icon = Icons.Rounded.Merge,
                        accentColor = RoxyColor.OrganizeAccent,
                        enabled = s.files.size >= 2,
                        onClick = onRequestSaveDestination
                    )
                }
                else -> {}
            }
        }
    ) { backdrop ->
        AnimatedContent(targetState = state, label = "merge_tool_state_switch") { currentState ->
            when (currentState) {
                is RoxyMergeState.EmptySelection -> {
                    EmptyDropzone(backdrop = backdrop, onPickClick = onPickFilesClick)
                }

                is RoxyMergeState.FilesSelected -> {
                    FilesList(
                        files = currentState.files,
                        totalPages = currentState.totalPages,
                        backdrop = backdrop,
                        onAddMoreClick = onPickFilesClick,
                        onRemove = { viewModel.removeDocument(it) },
                        onMoveUp = { index -> viewModel.moveDocument(index, index - 1) },
                        onMoveDown = { index -> viewModel.moveDocument(index, index + 1) }
                    )
                }

                is RoxyMergeState.Processing -> {
                    ProcessingView(
                        currentPage = currentState.currentPage,
                        totalPages = currentState.totalPages,
                        percent = currentState.progressPercent,
                        backdrop = backdrop
                    )
                }

                is RoxyMergeState.Success -> {
                    SuccessView(
                        outputUri = currentState.outputUri,
                        pages = currentState.mergedPages,
                        sizeBytes = currentState.outputSizeBytes,
                        backdrop = backdrop,
                        onOpen = { onOpenResultClick(currentState.outputUri) },
                        onShare = { onShareResultClick(currentState.outputUri) },
                        onNew = { viewModel.clear() }
                    )
                }

                is RoxyMergeState.Error -> {
                    ErrorView(
                        message = currentState.message,
                        backdrop = backdrop,
                        onRetry = { viewModel.clear() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDropzone(
    backdrop: RoxyBackdrop,
    onPickClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        RoxyGlassCard(
            backdrop = backdrop,
            shape = RoundedCornerShape(28.dp),
            scrimAlpha = 0.20f,
            blurRadius = 18.dp,
            onClick = onPickClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = CircleShape,
                    tintColor = RoxyColor.OrganizeAccent,
                    scrimAlpha = 0.35f,
                    interactive = false,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Files",
                        tint = RoxyColor.OrganizeAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Select PDFs to Merge",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap here to choose 2 or more files.\nFiles never leave your phone.",
                    color = RoxyColor.TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun FilesList(
    files: List<RoxyPdfDocument>,
    totalPages: Int,
    backdrop: RoxyBackdrop,
    onAddMoreClick: () -> Unit,
    onRemove: (RoxyPdfDocument) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${files.size} documents ($totalPages total pages)",
                    color = RoxyColor.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(12.dp),
                    tintColor = RoxyColor.OrganizeAccent,
                    scrimAlpha = 0.3f,
                    onClick = onAddMoreClick,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        itemsIndexed(files, key = { _, doc -> doc.uri }) { index, doc ->
            FileRowItem(
                index = index,
                totalFiles = files.size,
                doc = doc,
                backdrop = backdrop,
                onRemove = { onRemove(doc) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(88.dp)) // Space for floating button
        }
    }
}

@Composable
private fun FileRowItem(
    index: Int,
    totalFiles: Int,
    doc: RoxyPdfDocument,
    backdrop: RoxyBackdrop,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    RoxyGlassCard(
        backdrop = backdrop,
        shape = RoundedCornerShape(18.dp),
        scrimAlpha = 0.24f,
        blurRadius = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index & Icon
            RoxyGlassCard(
                backdrop = backdrop,
                shape = RoundedCornerShape(10.dp),
                tintColor = RoxyColor.OrganizeAccent,
                scrimAlpha = 0.35f,
                interactive = false,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = RoxyColor.OrganizeAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = "${doc.pageCount} pages • ${doc.formattedSize}",
                    color = RoxyColor.TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Move Up/Down Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (index > 0) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = RoxyColor.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onMoveUp)
                    )
                }
                if (index < totalFiles - 1) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = RoxyColor.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onMoveDown)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Remove Button
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove",
                tint = Color(0xFFEF4444),
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun ProcessingView(
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
                    color = RoxyColor.OrganizeAccent,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Merging Documents...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (totalPages > 0) "Processing page $currentPage of $totalPages" else "Preparing stream...",
                    color = RoxyColor.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SuccessView(
    outputUri: String,
    pages: Int,
    sizeBytes: Long,
    backdrop: RoxyBackdrop,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onNew: () -> Unit
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
                    text = "Merge Complete!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "$pages pages successfully merged.",
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
                        accentColor = RoxyColor.OrganizeAccent,
                        onClick = onOpen,
                        modifier = Modifier.weight(1f)
                    )

                    RoxyGlassButton(
                        text = "Share",
                        backdrop = backdrop,
                        icon = Icons.Rounded.Share,
                        accentColor = RoxyColor.Secondary,
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Merge another document",
                    color = RoxyColor.OrganizeAccent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onNew)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
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
                    text = "Operation Failed",
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
                    text = "Try Again",
                    backdrop = backdrop,
                    accentColor = Color(0xFFEF4444),
                    onClick = onRetry
                )
            }
        }
    }
}
