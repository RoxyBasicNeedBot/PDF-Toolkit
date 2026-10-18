package com.roxy.pdftoolkit.ui.tools.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import com.roxy.pdftoolkit.engine.scanner.RoxyDocumentQuad
import com.roxy.pdftoolkit.engine.scanner.RoxyScanFilterType
import com.roxy.pdftoolkit.engine.scanner.RoxyScannedPage
import com.roxy.pdftoolkit.engine.scanner.RoxyScannerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoxyScanViewModel(
    private val scannerEngine: RoxyScannerEngine
) : ViewModel() {

    private val pagesList = mutableListOf<RoxyScannedPage>()
    private val _state = MutableStateFlow<RoxyScanState>(RoxyScanState.CameraCapturing())
    val state: StateFlow<RoxyScanState> = _state.asStateFlow()

    fun onPageCaptured(rawImageBytes: ByteArray) {
        RoxyCrashTracker.logAction("onPageCaptured byte size: ${rawImageBytes.size}", category = "scan")
        viewModelScope.launch {
            try {
                val detectedQuad = scannerEngine.detectDocumentQuad(rawImageBytes)
                val initialPage = RoxyScannedPage(
                    id = "page_${pagesList.size + 1}_${System.currentTimeMillis()}",
                    originalImageBytes = rawImageBytes,
                    quad = detectedQuad,
                    filterType = RoxyScanFilterType.MAGIC_CLEAN
                )

                val processedBytes = scannerEngine.warpAndFilterPage(initialPage)
                val readyPage = initialPage.copy(processedImageBytes = processedBytes)
                pagesList.add(readyPage)

                _state.value = RoxyScanState.ReviewingPage(
                    activePageIndex = pagesList.size - 1,
                    page = readyPage,
                    totalPages = pagesList.size
                )
            } catch (e: Exception) {
                RoxyCrashTracker.captureException(e, tag = "OnPageCaptured")
            }
        }
    }

    fun setFilter(pageIndex: Int, filterType: RoxyScanFilterType) {
        if (pageIndex !in pagesList.indices) return
        viewModelScope.launch {
            val page = pagesList[pageIndex].copy(filterType = filterType)
            val newProcessed = scannerEngine.warpAndFilterPage(page)
            val updated = page.copy(processedImageBytes = newProcessed)
            pagesList[pageIndex] = updated

            _state.value = RoxyScanState.ReviewingPage(
                activePageIndex = pageIndex,
                page = updated,
                totalPages = pagesList.size
            )
        }
    }

    fun setQuad(pageIndex: Int, newQuad: RoxyDocumentQuad) {
        if (pageIndex !in pagesList.indices) return
        viewModelScope.launch {
            val page = pagesList[pageIndex].copy(quad = newQuad)
            val newProcessed = scannerEngine.warpAndFilterPage(page)
            val updated = page.copy(processedImageBytes = newProcessed)
            pagesList[pageIndex] = updated

            _state.value = RoxyScanState.ReviewingPage(
                activePageIndex = pageIndex,
                page = updated,
                totalPages = pagesList.size
            )
        }
    }

    fun resumeCapturing() {
        _state.value = RoxyScanState.CameraCapturing(pages = pagesList.toList())
    }

    fun deleteCurrentPage(pageIndex: Int) {
        if (pageIndex in pagesList.indices) {
            pagesList.removeAt(pageIndex)
            if (pagesList.isEmpty()) {
                _state.value = RoxyScanState.CameraCapturing()
            } else {
                val nextIndex = (pageIndex - 1).coerceAtLeast(0)
                _state.value = RoxyScanState.ReviewingPage(
                    activePageIndex = nextIndex,
                    page = pagesList[nextIndex],
                    totalPages = pagesList.size
                )
            }
        }
    }

    fun compileDocument(outputDestinationUri: String) {
        RoxyCrashTracker.logAction("compileDocument with ${pagesList.size} pages", category = "scan")
        if (pagesList.isEmpty()) {
            _state.value = RoxyScanState.Error("No pages scanned. Please scan at least one page.")
            return
        }

        viewModelScope.launch {
            try {
                scannerEngine.compileToPdf(pagesList, outputDestinationUri).collectLatest { result ->
                    when (result) {
                        is RoxyOperationResult.Progress -> {
                            _state.value = RoxyScanState.Compiling(
                                currentPage = result.currentPage,
                                totalPages = result.totalPages,
                                progressPercent = result.percent
                            )
                        }
                        is RoxyOperationResult.Success -> {
                            RoxyCrashTracker.logAction("compileDocument success: ${result.pageCount} pages", category = "scan")
                            _state.value = RoxyScanState.Success(
                                outputUri = result.outputUri,
                                pageCount = result.pageCount
                            )
                        }
                        is RoxyOperationResult.Error -> {
                            RoxyCrashTracker.logAction("compileDocument error: ${result.userFriendlyMessage}", category = "scan")
                            _state.value = RoxyScanState.Error(result.userFriendlyMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                RoxyCrashTracker.captureException(e, tag = "CompileDocument")
                _state.value = RoxyScanState.Error("Compilation failed: ${e.message}")
            }
        }
    }

    fun reset() {
        pagesList.clear()
        _state.value = RoxyScanState.CameraCapturing()
    }
}
