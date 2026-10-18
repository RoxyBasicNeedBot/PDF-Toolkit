package com.roxy.pdftoolkit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.roxy.pdftoolkit.saf.rememberRoxySafPdfPicker
import com.roxy.pdftoolkit.saf.rememberRoxySafPdfSaver
import com.roxy.pdftoolkit.ui.RoxyApp
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import org.koin.android.ext.android.inject

class RoxyMainActivity : ComponentActivity() {

    private val incomingUris = mutableStateListOf<Uri>()
    private val mergeViewModel: RoxyMergeViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        RoxyCrashTracker.logAction("RoxyMainActivity onCreate", category = "lifecycle")

        handleIncomingIntent(intent)

        setContent {
            // SAF Pick PDFs Launcher
            val pickPdfsLauncher = rememberRoxySafPdfPicker { selectedUris ->
                mergeViewModel.addDocuments(selectedUris.map { it.toString() })
            }

            // SAF Save Merged PDF Destination Launcher
            val savePdfLauncher = rememberRoxySafPdfSaver(suggestedFileName = "Roxy_Merged.pdf") { outputUri ->
                mergeViewModel.startMerge(outputUri.toString())
            }

            RoxyApp(
                initialSharedUris = incomingUris.map { it.toString() },
                onPickFilesRequest = { pickPdfsLauncher() },
                onSaveFileRequest = { savePdfLauncher() },
                onShareFileRequest = { uriStr -> sharePdfFile(uriStr) },
                onOpenFileRequest = { uriStr -> openPdfFile(uriStr) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    incomingUris.clear()
                    incomingUris.add(uri)
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    incomingUris.clear()
                    incomingUris.add(uri)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { list ->
                    incomingUris.clear()
                    incomingUris.addAll(list)
                }
            }
        }
    }

    private fun sharePdfFile(uriStr: String) {
        val uri = Uri.parse(uriStr)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
    }

    private fun openPdfFile(uriStr: String) {
        val uri = Uri.parse(uriStr)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(openIntent)
    }
}
