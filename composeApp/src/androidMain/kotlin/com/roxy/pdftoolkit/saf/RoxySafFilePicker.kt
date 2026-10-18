package com.roxy.pdftoolkit.saf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Storage Access Framework (SAF) file picker for selecting one or more PDF files.
 */
@Composable
fun rememberRoxySafPdfPicker(
    onPdfsSelected: (List<Uri>) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onPdfsSelected(uris)
        }
    }

    return {
        launcher.launch(arrayOf("application/pdf"))
    }
}
