package com.roxy.pdftoolkit.ui.tools.merge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
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
            updateFilesState()
        }
    }

    fun removeDocument(document: RoxyPdfDocument) {
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
        selectedDocuments.clear()
        _state.value = RoxyMergeState.EmptySelection
    }

    fun startMerge(outputDestinationUri: String) {
        if (selectedDocuments.size < 2) {
            _state.value = RoxyMergeState.Error("Please select at least 2 PDF files to merge.")
            return
        }

        viewModelScope.launch {
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
                        _state.value = RoxyMergeState.Success(
                            outputUri = result.outputUri,
                            mergedPages = result.pageCount,
                            outputSizeBytes = result.fileSizeBytes
                        )
                    }
                    is RoxyOperationResult.Error -> {
                        _state.value = RoxyMergeState.Error(result.userFriendlyMessage)
                    }
                }
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
