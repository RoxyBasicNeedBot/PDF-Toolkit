package com.roxy.pdftoolkit.ui.tools.merge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.roxy.pdftoolkit.engine.RoxyPdfEngine
import com.roxy.pdftoolkit.engine.model.RoxyOperationResult
import com.roxy.pdftoolkit.engine.model.RoxyPdfDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoxyMergeViewModel(
    private val pdfEngine: RoxyPdfEngine
) : ViewModel() {

    private val _state = MutableStateFlow<RoxyMergeState>(RoxyMergeState.EmptySelection)
    val state: StateFlow<RoxyMergeState> = _state.asStateFlow()

    private val selectedDocuments = mutableListOf<RoxyPdfDocument>()

    fun addDocuments(uris: List<String>) {
        RoxyCrashTracker.logAction("Merge addDocuments: ${uris.size} URIs", category = "merge")
        viewModelScope.launch {
            try {
                for (uri in uris) {
                // Avoid duplicates
                if (selectedDocuments.none { it.uri == uri }) {
                    val metaResult = pdfEngine.getDocumentMetadata(uri)
                    val doc = metaResult.getOrElse {
                        val count = pdfEngine.getPageCount(uri)
                        RoxyPdfDocument(
                            uri = uri,
                            displayName = uri.substringAfterLast("/"),
                            sizeBytes = 0L,
                            pageCount = count
                        )
                    }
                    selectedDocuments.add(doc)
                }
            }
            } catch (e: Exception) {
                RoxyCrashTracker.captureException(e, tag = "MergeAddDocs")
            }
            updateFilesState()
        }
    }

    fun removeDocument(document: RoxyPdfDocument) {
        RoxyCrashTracker.logAction("Merge removeDocument: ${document.displayName}", category = "merge")
        selectedDocuments.remove(document)
        updateFilesState()
    }

    fun moveDocument(fromIndex: Int, toIndex: Int) {
        if (fromIndex in selectedDocuments.indices && toIndex in selectedDocuments.indices) {
            val item = selectedDocuments.removeAt(fromIndex)
            selectedDocuments.add(toIndex, item)
            updateFilesState()
        }
    }

    fun clear() {
        RoxyCrashTracker.logAction("Merge clear", category = "merge")
        selectedDocuments.clear()
        _state.value = RoxyMergeState.EmptySelection
    }

    fun startMerge(outputDestinationUri: String) {
        RoxyCrashTracker.logAction("startMerge invoked with ${selectedDocuments.size} docs", category = "merge")
        if (selectedDocuments.size < 2) {
            _state.value = RoxyMergeState.Error("Please select at least 2 PDF files to merge.")
            return
        }

        viewModelScope.launch {
            try {
                val inputUris = selectedDocuments.map { it.uri }
                pdfEngine.mergePdfs(inputUris, outputDestinationUri).collectLatest { result ->
                    when (result) {
                        is RoxyOperationResult.Progress -> {
                            _state.value = RoxyMergeState.Processing(
                                currentPage = result.currentPage,
                                totalPages = result.totalPages,
                                progressPercent = result.percent
                            )
                        }
                        is RoxyOperationResult.Success -> {
                            RoxyCrashTracker.logAction("Merge success: ${result.pageCount} pages", category = "merge")
                            _state.value = RoxyMergeState.Success(
                                outputUri = result.outputUri,
                                mergedPages = result.pageCount,
                                outputSizeBytes = result.fileSizeBytes
                            )
                        }
                        is RoxyOperationResult.Error -> {
                            RoxyCrashTracker.logAction("Merge error: ${result.userFriendlyMessage}", category = "merge")
                            _state.value = RoxyMergeState.Error(result.userFriendlyMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                RoxyCrashTracker.captureException(e, tag = "StartMergeExecution")
                _state.value = RoxyMergeState.Error("An unexpected error occurred during merge: ${e.message}")
            }
        }
    }

    private fun updateFilesState() {
        if (selectedDocuments.isEmpty()) {
            _state.value = RoxyMergeState.EmptySelection
        } else {
            val totalPages = selectedDocuments.sumOf { it.pageCount }
            _state.value = RoxyMergeState.FilesSelected(
                files = selectedDocuments.toList(),
                totalPages = totalPages
            )
        }
    }
}
