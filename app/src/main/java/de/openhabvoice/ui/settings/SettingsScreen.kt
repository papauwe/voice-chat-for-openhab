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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openhabvoice.R
import de.openhabvoice.data.AppSettings
import de.openhabvoice.data.AppTheme
import de.openhabvoice.data.ChatMode
import de.openhabvoice.data.SttEngine

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
    var sttEngine by remember { mutableStateOf(current.sttEngine) }
    var appTheme by remember { mutableStateOf(current.appTheme) }
    var modeMenuOpen by remember { mutableStateOf(false) }
    var engineMenuOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        bottomBar = {
            // Sticky footer: the save button stays visible at all times (even while scrolling)
            // and avoids the keyboard (imePadding) as well as the gesture bar (navigationBarsPadding).
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
                                sttLanguage = sttLanguage.ifBlank { "de-DE" },
                                sttEngine = sttEngine,
                                appTheme = appTheme
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.settings_save))
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
            SectionTitle(stringResource(R.string.settings_section_server))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(stringResource(R.string.settings_server_url)) },
                supportingText = { Text(stringResource(R.string.settings_server_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = modeMenuOpen,
                onExpandedChange = { modeMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = stringResource(chatMode.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_chat_mode)) },
                    supportingText = { Text(stringResource(chatMode.descRes)) },
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
                                    Text(stringResource(mode.labelRes))
                                    Text(
                                        stringResource(mode.descRes),
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
                    label = { Text(stringResource(R.string.settings_custom_path)) },
                    supportingText = { Text(stringResource(R.string.settings_custom_path_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle(stringResource(R.string.settings_section_auth))

            OutlinedTextField(
                value = apiToken,
                onValueChange = { apiToken = it },
                label = { Text(stringResource(R.string.settings_api_token)) },
                supportingText = { Text(stringResource(R.string.settings_api_token_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.settings_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.settings_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(stringResource(R.string.settings_section_language))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_tts), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_tts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = ttsEnabled, onCheckedChange = { ttsEnabled = it })
            }

            OutlinedTextField(
                value = sttLanguage,
                onValueChange = { sttLanguage = it },
                label = { Text(stringResource(R.string.settings_stt_language)) },
                supportingText = { Text(stringResource(R.string.settings_stt_language_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = engineMenuOpen,
                onExpandedChange = { engineMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = stringResource(sttEngine.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_stt_engine)) },
                    supportingText = { Text(stringResource(R.string.settings_stt_engine_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = engineMenuOpen,
                    onDismissRequest = { engineMenuOpen = false }
                ) {
                    SttEngine.entries.forEach { engine ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(engine.labelRes))
                                    Text(
                                        stringResource(engine.descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                sttEngine = engine
                                engineMenuOpen = false
                            }
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_section_appearance))

            ExposedDropdownMenuBox(
                expanded = themeMenuOpen,
                onExpandedChange = { themeMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = stringResource(appTheme.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_app_theme)) },
                    supportingText = { Text(stringResource(R.string.settings_app_theme_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = themeMenuOpen,
                    onDismissRequest = { themeMenuOpen = false }
                ) {
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(theme.labelRes))
                                    Text(
                                        stringResource(theme.descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                appTheme = theme
                                themeMenuOpen = false
                            }
                        )
                    }
                }
            }

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

/** Display texts of the chat modes as string resources (localized instead of in the enum). */
private val ChatMode.labelRes: Int
    get() = when (this) {
        ChatMode.OPENHAB5_CHAT -> R.string.mode_openhab5_label
        ChatMode.HABOT -> R.string.mode_habot_label
        ChatMode.VOICE -> R.string.mode_voice_label
        ChatMode.CUSTOM -> R.string.mode_custom_label
    }

private val ChatMode.descRes: Int
    get() = when (this) {
        ChatMode.OPENHAB5_CHAT -> R.string.mode_openhab5_desc
        ChatMode.HABOT -> R.string.mode_habot_desc
        ChatMode.VOICE -> R.string.mode_voice_desc
        ChatMode.CUSTOM -> R.string.mode_custom_desc
    }

/** Display texts of the STT engines as string resources (localized instead of in the enum). */
private val SttEngine.labelRes: Int
    get() = when (this) {
        SttEngine.AUTO -> R.string.stt_engine_auto_label
        SttEngine.SYSTEM -> R.string.stt_engine_system_label
        SttEngine.OFFLINE -> R.string.stt_engine_offline_label
    }

private val SttEngine.descRes: Int
    get() = when (this) {
        SttEngine.AUTO -> R.string.stt_engine_auto_desc
        SttEngine.SYSTEM -> R.string.stt_engine_system_desc
        SttEngine.OFFLINE -> R.string.stt_engine_offline_desc
    }

/** Display texts of the app themes as string resources (localized instead of in the enum). */
private val AppTheme.labelRes: Int
    get() = when (this) {
        AppTheme.OPENHAB -> R.string.theme_openhab_label
        AppTheme.BLUE -> R.string.theme_blue_label
        AppTheme.GREEN -> R.string.theme_green_label
        AppTheme.VIOLET -> R.string.theme_violet_label
        AppTheme.DYNAMIC -> R.string.theme_dynamic_label
    }

private val AppTheme.descRes: Int
    get() = when (this) {
        AppTheme.OPENHAB -> R.string.theme_openhab_desc
        AppTheme.BLUE -> R.string.theme_blue_desc
        AppTheme.GREEN -> R.string.theme_green_desc
        AppTheme.VIOLET -> R.string.theme_violet_desc
        AppTheme.DYNAMIC -> R.string.theme_dynamic_desc
    }
