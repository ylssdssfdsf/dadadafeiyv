package com.dafeiyu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dafeiyu.app.ui.ChatScreen
import com.dafeiyu.app.ui.ChatViewModel
import com.dafeiyu.app.ui.SettingsScreen
import com.dafeiyu.app.ui.theme.DaFeiYuTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    private val _incomingFiles = MutableStateFlow<List<Uri>?>(null)
    val incomingFiles: StateFlow<List<Uri>?> = _incomingFiles

    private val pickFiles = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) _incomingFiles.value = uris
    }

    fun openFilePicker() {
        pickFiles.launch(arrayOf("*/*"))
    }

    fun consumeIncomingFiles() {
        _incomingFiles.value = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        dispatch(intent)

        setContent {
            DaFeiYuTheme {
                val vm: ChatViewModel = viewModel()
                val activity = LocalContext.current as MainActivity
                var showSettings by remember { mutableStateOf(false) }
                val pending by activity.incomingFiles.collectAsState()

                LaunchedEffect(pending) {
                    val uris = pending
                    if (!uris.isNullOrEmpty()) {
                        vm.ingestFiles(uris)
                        activity.consumeIncomingFiles()
                    }
                }

                if (showSettings) {
                    SettingsScreen(vm = vm, onBack = { showSettings = false })
                } else {
                    ChatScreen(vm = vm, onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatch(intent)
    }

    private fun dispatch(source: Intent?) {
        val uris = source?.let(::extractUris).orEmpty()
        if (uris.isNotEmpty()) _incomingFiles.value = uris
    }

    private fun extractUris(intent: Intent): List<Uri> {
        val result = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        ?.let { result += it }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { result += it }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        ?.let { result += it }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                        ?.let { result += it }
                }
            }

            Intent.ACTION_VIEW -> intent.data?.let { result += it }
        }

        // 有些应用只塞 clipData，不填 EXTRA_STREAM。
        if (result.isEmpty()) {
            intent.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { result += it }
                }
            }
        }
        return result.distinct()
    }
}
