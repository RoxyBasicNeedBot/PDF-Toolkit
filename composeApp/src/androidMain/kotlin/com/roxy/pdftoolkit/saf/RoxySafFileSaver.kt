package com.roxy.pdftoolkit.saf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Storage Access Framework (SAF) file saver for creating a new PDF file location.
 */
@Composable
fun rememberRoxySafPdfSaver(
    suggestedFileName: String = "Roxy_Merged.pdf",
    onDestinationSelected: (Uri) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onDestinationSelected(uri)
        }
    }

    return {
        launcher.launch(suggestedFileName)
    }
}
