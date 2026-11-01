package de.openhabvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.openhabvoice.ui.chat.ChatScreen
import de.openhabvoice.ui.settings.SettingsScreen
import de.openhabvoice.ui.theme.OpenHabVoiceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenHabVoiceTheme {
                var showSettings by remember { mutableStateOf(false) }

                // Widget-Aufruf: beim Öffnen sofort mit dem Zuhören beginnen
                if (intent.getBooleanExtra(EXTRA_AUTO_START_LISTENING, false)) {
                    AutoStartListeningEffect(viewModel)
                }

                if (showSettings) {
                    SettingsScreen(
                        current = viewModel.settings.value,
                        onSave = { viewModel.saveSettings(it) },
                        onBack = { showSettings = false }
                    )
                } else {
                    ChatScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    companion object {
        /** Extra-Flag vom Home-Screen-Widget: Spracherkennung sofort starten. */
        const val EXTRA_AUTO_START_LISTENING = "de.openhabvoice.AUTO_START_LISTENING"
    }
}

/**
 * Prueft die Mikrofon-Berechtigung und startet direkt die Spracherkennung.
 * Wird durch das Widget-Extra [MainActivity.EXTRA_AUTO_START_LISTENING] ausgeloest.
 */
@Composable
private fun AutoStartListeningEffect(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onMicClick()
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onMicClick()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
