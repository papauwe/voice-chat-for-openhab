package de.openhabvoice.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.openhabvoice.data.AppSettings
import de.openhabvoice.data.ChatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    current: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(current.serverUrl) }
    var chatMode by remember { mutableStateOf(current.chatMode) }
    var customPath by remember { mutableStateOf(current.customPath) }
    var apiToken by remember { mutableStateOf(current.apiToken) }
    var username by remember { mutableStateOf(current.username) }
    var password by remember { mutableStateOf(current.password) }
    var ttsEnabled by remember { mutableStateOf(current.ttsEnabled) }
    var sttLanguage by remember { mutableStateOf(current.sttLanguage) }
    var modeMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            // Sticky Footer: Save-Button bleibt immer sichtbar (auch beim Scrollen)
            // und weicht Tastatur (imePadding) sowie Gestenleiste (navigationBarsPadding) aus.
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        onSave(
                            AppSettings(
                                serverUrl = serverUrl,
                                chatMode = chatMode,
                                customPath = customPath,
                                apiToken = apiToken,
                                username = username,
                                password = password,
                                ttsEnabled = ttsEnabled,
                                sttLanguage = sttLanguage.ifBlank { "de-DE" }
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text("Speichern")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("openHAB Server")

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server-URL") },
                supportingText = { Text("z. B. http://192.168.1.10:8080 oder https://openhab.example.org") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = modeMenuOpen,
                onExpandedChange = { modeMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = chatMode.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Chat-Modus") },
                    supportingText = { Text(chatMode.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = modeMenuOpen,
                    onDismissRequest = { modeMenuOpen = false }
                ) {
                    ChatMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.label)
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                chatMode = mode
                                if (mode != ChatMode.CUSTOM && customPath.isBlank()) {
                                    customPath = mode.defaultPath
                                }
                                modeMenuOpen = false
                            }
                        )
                    }
                }
            }

            if (chatMode == ChatMode.CUSTOM) {
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it },
                    label = { Text("Endpoint-Pfad") },
                    supportingText = { Text("z. B. /rest/habot/chat – POST mit {\"message\": …}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("Authentifizierung (optional)")

            OutlinedTextField(
                value = apiToken,
                onValueChange = { apiToken = it },
                label = { Text("API-Token") },
                supportingText = { Text("openHAB API-Token (Bearer / X-OpenHAB-Token)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Benutzer (Basic Auth)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Sprache")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sprachausgabe (TTS)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Antworten vorlesen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = ttsEnabled, onCheckedChange = { ttsEnabled = it })
            }

            OutlinedTextField(
                value = sttLanguage,
                onValueChange = { sttLanguage = it },
                label = { Text("Sprache der Erkennung") },
                supportingText = { Text("BCP-47-Tag, z. B. de-DE oder en-US") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
