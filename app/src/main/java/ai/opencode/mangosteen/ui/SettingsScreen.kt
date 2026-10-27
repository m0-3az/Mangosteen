package ai.opencode.mangosteen.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.opencode.mangosteen.ui.components.MangosteenBackground
import ai.opencode.mangosteen.ui.components.ModelPickerSheet
import ai.opencode.mangosteen.ui.components.PrimaryButton
import ai.opencode.mangosteen.ui.components.RoundIconButton
import ai.opencode.mangosteen.ui.components.SettingsRow
import ai.opencode.mangosteen.ui.components.SettingsSectionLabel
import ai.opencode.mangosteen.ui.components.SettingsToggleRow
import ai.opencode.mangosteen.ui.components.SheetHeading
import ai.opencode.mangosteen.ui.components.ThemePickerSheet
import ai.opencode.mangosteen.ui.components.ThemeSwatches
import ai.opencode.mangosteen.ui.theme.ThemeCatalog
import ai.opencode.mangosteen.ui.theme.ThemeStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    var showApiKey by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var showChatPicker by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var apiDraft by remember { mutableStateOf("") }
    var promptDraft by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadModels() }
    LaunchedEffect(viewModel.toast) {
        viewModel.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.toast = null
        }
    }

    MangosteenBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Make it yours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsSectionLabel("Appearance")

            val theme = ThemeCatalog.byId(ThemeStore.themeId)
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = "Theme",
                subtitle = theme.name + if (theme.isDark) " · Dark" else " · Light",
                onClick = { showTheme = true },
                trailing = { ThemeSwatches(theme.swatches, size = 16.dp) },
            )

            SettingsSectionLabel("Connection")

            SettingsRow(
                icon = Icons.Rounded.Key,
                title = "API key",
                subtitle = if (viewModel.apiKey.isBlank()) "Not set" else "Saved · OpenCode Go",
                onClick = {
                    apiDraft = viewModel.apiKey
                    showApiKey = true
                },
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = "Default model",
                subtitle = viewModel.modelLabel(),
                onClick = {
                    viewModel.loadModels()
                    showModels = true
                },
            )

            SettingsSectionLabel("Persona")

            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = "System prompt",
                subtitle = viewModel.systemPrompt
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?.let { if (it.length > 52) it.take(52) + "…" else it }
                    ?: "Applies to every chat",
                onClick = {
                    promptDraft = viewModel.systemPrompt
                    showPrompt = true
                },
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Extract a character",
                subtitle = if (viewModel.extracting) "Extracting…" else "From an existing conversation",
                onClick = {
                    if (!viewModel.extracting) {
                        viewModel.loadChats()
                        showChatPicker = true
                    }
                },
            )

            SettingsSectionLabel("Check-ins")

            SettingsToggleRow(
                icon = Icons.Rounded.Schedule,
                title = "Proactive check-ins",
                subtitle = "Occasional gentle nudges",
                checked = viewModel.proactive,
                onCheckedChange = { viewModel.proactive = it },
            )
            Spacer(Modifier.height(10.dp))
            SettingsToggleRow(
                icon = Icons.Rounded.Notifications,
                title = "Notifications",
                subtitle = "Only when something needs you",
                checked = viewModel.notifications,
                onCheckedChange = { viewModel.notifications = it },
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { confirmClear = true }
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Clear all chats",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(text = "Save changes", onClick = { viewModel.save(onBack) })

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Mangosteen 2.1 · Material 3 Expressive",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTheme) {
        ThemePickerSheet(
            selectedId = ThemeStore.themeId,
            onSelect = { ThemeStore.set(context, it) },
            onDismiss = { showTheme = false },
        )
    }

    if (showApiKey) {
        AlertDialog(
            onDismissRequest = { showApiKey = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "API key",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = apiDraft,
                        onValueChange = { apiDraft = it },
                        singleLine = true,
                        placeholder = { Text("Paste your OpenCode Go API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Leave empty to remove the saved key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.apiKey = apiDraft
                    showApiKey = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKey = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "System prompt",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    placeholder = { Text("Applies to every chat") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveSystemPrompt(promptDraft)
                    showPrompt = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrompt = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showModels) {
        ModelPickerSheet(
            models = viewModel.modelOptions,
            selectedId = viewModel.modelId,
            onApply = {
                viewModel.modelId = it
                showModels = false
            },
            onDismiss = { showModels = false },
            title = "Default model",
            subtitle = "Used for new chats. Each chat can still override it.",
        )
    }

    if (showChatPicker) {
        ModalBottomSheet(
            onDismissRequest = { showChatPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
            ) {
                SheetHeading(
                    title = "Extract a character",
                    subtitle = "Choose a chat to learn its voice from.",
                )
                Spacer(Modifier.height(16.dp))
                if (viewModel.chats.isEmpty()) {
                    Text(
                        text = "No chats yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(viewModel.chats, key = { it.id }) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable {
                                        showChatPicker = false
                                        viewModel.extractVoice(chat)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = chat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Clear all chats?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "Every conversation, message and reminder will be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllChats()
                    confirmClear = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
