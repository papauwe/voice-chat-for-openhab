package de.openhabvoice.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.openhabvoice.ChatViewModel
import de.openhabvoice.data.ChatMessage
import androidx.compose.ui.unit.Dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val partialText by viewModel.partialText.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ttsEnabled = settings.ttsEnabled

    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onMicClick()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("openHAB Voice", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (settings.serverUrl.isBlank()) "Server nicht konfiguriert" else settings.serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSettings(viewModel.settings.value.copy(ttsEnabled = !ttsEnabled)) }) {
                        Icon(
                            imageVector = if (ttsEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = if (ttsEnabled) "Sprachausgabe aus" else "Sprachausgabe an"
                        )
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Chat leeren")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                viewModel = viewModel,
                isListening = isListening,
                partialText = partialText,
                isSending = isSending,
                speechError = speechError,
                onMicClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.onMicClick()
                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
            if (isSending) {
                item(key = "typing") { TypingIndicator() }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val timeFormat = remember(message.id) { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    when (message.role) {
        ChatMessage.Role.SYSTEM -> {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
        ChatMessage.Role.USER -> Bubble(
            text = message.text,
            time = timeFormat.format(Date(message.timestamp)),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            alignEnd = true
        )
        ChatMessage.Role.ASSISTANT -> Bubble(
            text = message.text,
            time = timeFormat.format(Date(message.timestamp)),
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            alignEnd = false,
            markdown = true,
            maxWidth = 340.dp
        )
    }
}

@Composable
private fun Bubble(
    text: String,
    time: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean,
    markdown: Boolean = false,
    maxWidth: Dp = 300.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (alignEnd) 18.dp else 4.dp,
                bottomEnd = if (alignEnd) 4.dp else 18.dp
            ),
            color = container,
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (markdown) {
                    MarkdownMessage(text, content)
                } else {
                    Text(text, color = content, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "openHAB denkt …",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    viewModel: ChatViewModel,
    isListening: Boolean,
    partialText: String,
    isSending: Boolean,
    speechError: String?,
    onMicClick: () -> Unit
) {
    var input by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        // Freien Platz unterhalb der Gestenleiste lassen, damit der untere
        // Rahmen des Eingabefelds nicht hinter dem System-Drawer verschwindet.
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (speechError != null) {
                Text(
                    text = speechError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = if (isListening && partialText.isNotBlank()) partialText else input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                isListening -> "Ich höre zu …"
                                isSending -> "Warte auf openHAB …"
                                else -> "Nachricht an openHAB …"
                            }
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    readOnly = isListening
                )

                Spacer(Modifier.width(8.dp))

                MicButton(
                    isListening = isListening,
                    enabled = !isSending,
                    onClick = onMicClick
                )

                Spacer(Modifier.width(4.dp))

                androidx.compose.material3.FilledIconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            input = ""
                            viewModel.sendText(text)
                        }
                    },
                    enabled = !isSending && input.isNotBlank() && !isListening,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Senden")
                }
            }
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, enabled: Boolean, onClick: () -> Unit) {
    // Puls-Animation nur erzeugen, wenn tatsaechlich gehoert wird: Die unendliche
    // Transition haelt sonst dauerhaft den Compose-FrameClock-Loop am Laufen und
    // Robolectric-Tests (ShadowPausedLooper.idle) kommen nie in den Idle-Zustand.
    // Auch produktiv korrekt: Der Puls ist nur beim aktiven Zuhören sichtbar.
    if (isListening) {
        val transition = rememberInfiniteTransition(label = "micPulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "scale"
        )
        MicButtonContent(isListening, enabled, scale, onClick)
    } else {
        MicButtonContent(isListening, enabled, 1f, onClick)
    }
}

@Composable
private fun MicButtonContent(
    isListening: Boolean,
    enabled: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    androidx.compose.material3.FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .scale(scale),
        shape = CircleShape,
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isListening) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            contentColor = if (isListening) MaterialTheme.colorScheme.onError
            else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isListening) "Aufnahme stoppen" else "Sprechen",
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
        )
    }
}
