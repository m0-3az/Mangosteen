package ai.opencode.mangosteen.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.opencode.mangosteen.net.ModelCatalog
import ai.opencode.mangosteen.net.Reasoning
import ai.opencode.mangosteen.proactive.CheckinText
import ai.opencode.mangosteen.ui.components.HeroMark
import ai.opencode.mangosteen.ui.components.MangosteenAvatar
import ai.opencode.mangosteen.ui.components.MangosteenBackground
import ai.opencode.mangosteen.ui.components.ModelPickerSheet
import ai.opencode.mangosteen.ui.components.RoundIconButton
import ai.opencode.mangosteen.ui.components.SheetActionRow
import ai.opencode.mangosteen.ui.components.SheetHeading
import ai.opencode.mangosteen.ui.components.SoftDivider
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class SlashCommand(
    val name: String,
    val hint: String,
    val takesArgs: Boolean = false,
    val icon: ImageVector,
)

private val SLASH_COMMANDS = listOf(
    SlashCommand("/system", "Set the system prompt", icon = Icons.Rounded.Tune),
    SlashCommand("/checkin", "Schedule a check-in", takesArgs = true, icon = Icons.Rounded.Schedule),
    SlashCommand("/clear", "Clear this chat", icon = Icons.Rounded.DeleteSweep),
    SlashCommand("/delete", "Delete this chat", icon = Icons.Rounded.DeleteOutline),
)

private data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val prompt: String,
)

private val STARTER_PROMPTS = listOf(
    StarterPrompt(
        "Plan my week", "Turn a list into a plan", Icons.Rounded.CalendarMonth,
        "Help me plan my week. Here's what I have on: ",
    ),
    StarterPrompt(
        "Explain a paper", "Summarize the key ideas", Icons.AutoMirrored.Rounded.MenuBook,
        "Explain this paper and summarize the key ideas: ",
    ),
    StarterPrompt(
        "Debug an error", "Find the root cause", Icons.Rounded.BugReport,
        "Help me debug this error and find the root cause: ",
    ),
    StarterPrompt(
        "Draft a message", "Make it sound human", Icons.Rounded.EditNote,
        "Help me draft a message that sounds natural. What I want to say: ",
    ),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    LaunchedEffect(conversationId) { viewModel.load(conversationId) }

    val context = LocalContext.current
    val messages = viewModel.messages
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    var input by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showReasoning by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<UiMessage?>(null) }
    var selecting by remember { mutableStateOf<UiMessage?>(null) }
    var editingAssistant by remember { mutableStateOf<UiMessage?>(null) }
    var pendingDelete by remember { mutableStateOf<UiMessage?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var showSystem by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.attachImage(uri) }

    LaunchedEffect(viewModel.editing) {
        viewModel.editing?.let { input = it.content }
    }
    LaunchedEffect(showModels) {
        if (showModels) viewModel.loadModels()
    }
    LaunchedEffect(viewModel.toast) {
        viewModel.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.toast = null
        }
    }
    val lastLength = messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(messages.size, lastLength, viewModel.streaming) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    val suggestions = if (
        viewModel.editing == null && input.startsWith("/") && !input.contains(' ')
    ) {
        SLASH_COMMANDS.filter { it.name.startsWith(input) }
    } else {
        emptyList()
    }

    fun submit() {
        val command = SLASH_COMMANDS.firstOrNull {
            input == it.name || input.startsWith(it.name + " ")
        }
        val argument = input.removePrefix(command?.name.orEmpty()).trim()
        when {
            command?.name == "/system" -> {
                input = ""
                showSystem = true
            }
            command?.name == "/checkin" -> {
                val fireAt = CheckinText.parse(argument)
                if (fireAt != null) {
                    viewModel.scheduleCheckIn(fireAt, argument)
                    input = ""
                } else {
                    viewModel.toast = "Add a time, like /checkin exam tomorrow at 9am"
                }
            }
            command?.name == "/clear" -> {
                input = ""
                confirmClear = true
            }
            command?.name == "/delete" -> {
                input = ""
                confirmDelete = true
            }
            input.isNotBlank() || viewModel.pendingAttachment != null -> {
                viewModel.send(input)
                input = ""
            }
        }
    }

    fun pickCommand(command: SlashCommand) {
        input = if (command.takesArgs) command.name + " " else command.name
        focusRequester.requestFocus()
    }

    MangosteenBackground {
        Column(Modifier.fillMaxSize()) {
            ChatTopBar(
                title = viewModel.conversation?.title?.ifBlank { ChatViewModel.NEW_TITLE }
                    ?: ChatViewModel.NEW_TITLE,
                subtitle = viewModel.conversation?.let { ModelCatalog.info(it.modelId).display }
                    ?: "",
                empty = messages.isEmpty(),
                onBack = onBack,
                onModel = { viewModel.loadModels(); showModels = true },
                onSlash = {
                    if (input.isEmpty()) input = "/"
                    focusRequester.requestFocus()
                },
                onMore = { showMore = true },
                modifier = Modifier.statusBarsPadding(),
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) {
                    NewChatState(
                        onPickPrompt = {
                            input = it
                            focusRequester.requestFocus()
                        },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
                    ) {
                        items(messages, key = { System.identityHashCode(it) }) { message ->
                            val index = messages.indexOf(message)
                            MessageItem(
                                message = message,
                                previous = if (index > 0) messages[index - 1] else null,
                                streaming = viewModel.streaming,
                                selected = actionMessage === message,
                                selecting = selecting === message,
                                onLongPress = { actionMessage = it },
                                onCopy = { copyToClipboard(context, it.content) },
                                onRegenerate = { viewModel.regenerate(it) },
                                onDoneSelect = { selecting = null },
                            )
                        }
                    }
                }

                if (suggestions.isNotEmpty()) {
                    CommandMenu(
                        commands = suggestions,
                        onPick = ::pickCommand,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            ComposerArea(
                input = input,
                onInputChange = { input = it },
                focusRequester = focusRequester,
                editing = viewModel.editing != null,
                onCancelEdit = {
                    viewModel.cancelEdit()
                    input = ""
                },
                attachmentPath = viewModel.pendingAttachment?.path,
                onRemoveAttachment = { viewModel.clearAttachment() },
                streaming = viewModel.streaming,
                reasoning = viewModel.reasoning,
                searchOn = viewModel.search,
                onReasoningClick = { showReasoning = true },
                onToggleSearch = { viewModel.toggleSearch() },
                onPickMedia = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSend = ::submit,
                onStop = { viewModel.stop() },
            )
        }
    }

    actionMessage?.let { message ->
        MessageActionsSheet(
            message = message,
            onDismiss = { actionMessage = null },
            onCopy = {
                copyToClipboard(context, message.content)
                actionMessage = null
            },
            onEdit = {
                actionMessage = null
                if (message.isUser) viewModel.startEdit(message) else editingAssistant = message
            },
            onRegenerate = {
                actionMessage = null
                viewModel.regenerate(message)
            },
            onSelect = {
                actionMessage = null
                selecting = message
            },
            onDelete = {
                actionMessage = null
                pendingDelete = message
            },
        )
    }

    if (showMore) {
        MoreSheet(
            onDismiss = { showMore = false },
            onRename = { showMore = false; showRename = true },
            onSystemPrompt = { showMore = false; showSystem = true },
            onCheckInHour = {
                showMore = false
                viewModel.scheduleCheckIn(viewModel.inOneHour())
            },
            onCheckInTomorrow = {
                showMore = false
                viewModel.scheduleCheckIn(viewModel.tomorrowMorning())
            },
            onClear = { showMore = false; confirmClear = true },
            onDelete = { showMore = false; confirmDelete = true },
        )
    }

    if (showReasoning) {
        ReasoningSheet(
            selected = viewModel.reasoning,
            onChoose = {
                viewModel.chooseReasoning(it)
                showReasoning = false
            },
            onDismiss = { showReasoning = false },
        )
    }

    if (showModels) {
        ModelPickerSheet(
            models = viewModel.modelOptions,
            selectedId = viewModel.conversation?.modelId.orEmpty(),
            onApply = {
                viewModel.applyModel(ModelCatalog.info(it))
                showModels = false
            },
            onDismiss = { showModels = false },
            onManage = {
                showModels = false
                onOpenSettings()
            },
        )
    }

    editingAssistant?.let { message ->
        TextInputDialog(
            title = "Edit reply",
            initial = message.content,
            singleLine = false,
            onDismiss = { editingAssistant = null },
            onConfirm = {
                viewModel.editAssistant(message, it)
                editingAssistant = null
            },
        )
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename chat",
            initial = viewModel.conversation?.title.orEmpty(),
            onDismiss = { showRename = false },
            onConfirm = { viewModel.rename(it); showRename = false },
        )
    }

    if (showSystem) {
        TextInputDialog(
            title = "System prompt",
            initial = viewModel.conversation?.systemPrompt.orEmpty(),
            singleLine = false,
            onDismiss = { showSystem = false },
            onConfirm = { viewModel.setSystemPrompt(it); showSystem = false },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear messages",
            message = "Remove all messages in this chat?",
            confirmLabel = "Clear",
            onDismiss = { confirmClear = false },
            onConfirm = {
                viewModel.clearMessages()
                confirmClear = false
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete chat",
            message = "Delete this chat for good?",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onBack)
            },
        )
    }

    pendingDelete?.let { message ->
        ConfirmDialog(
            title = "Delete message",
            message = "Delete this message and everything after it?",
            confirmLabel = "Delete",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteFrom(message)
                pendingDelete = null
                selecting = null
            },
        )
    }
}

// ------------------------------------------------------------------- header

@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    empty: Boolean,
    onBack: () -> Unit,
    onModel: () -> Unit,
    onSlash: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (!empty) {
            BareIconButton(Icons.Rounded.AutoAwesome, "Choose model", onModel)
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .semantics { contentDescription = "Slash commands" }
                    .clickable(onClick = onSlash),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "/",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        BareIconButton(Icons.Rounded.MoreVert, "More", onMore)
    }
}

@Composable
private fun BareIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(19.dp),
        )
    }
}

// ---------------------------------------------------------------- new chat

@Composable
private fun NewChatState(onPickPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(70.dp))
        HeroMark()
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Good evening",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "What's on your mind?",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ask anything, plan something, or pick up where you left off.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Try a prompt",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in STARTER_PROMPTS.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (prompt in row) {
                        PromptCard(
                            prompt = prompt,
                            onClick = { onPickPrompt(prompt.prompt) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PromptCard(
    prompt: StarterPrompt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = prompt.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = prompt.title,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = prompt.subtitle,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------- messages

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    message: UiMessage,
    previous: UiMessage?,
    streaming: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onLongPress: (UiMessage) -> Unit,
    onCopy: (UiMessage) -> Unit,
    onRegenerate: (UiMessage) -> Unit,
    onDoneSelect: () -> Unit,
) {
    val showSeparator = previous == null || separatorNeeded(previous.createdAt, message.createdAt)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (showSeparator) {
            Text(
                text = dayLabel(message.createdAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
        if (message.isUser) {
            UserMessage(
                message = message,
                selected = selected,
                selecting = selecting,
                onLongPress = onLongPress,
                onDoneSelect = onDoneSelect,
            )
        } else {
            AssistantMessage(
                message = message,
                streaming = streaming,
                selected = selected,
                selecting = selecting,
                onLongPress = onLongPress,
                onCopy = onCopy,
                onRegenerate = onRegenerate,
                onDoneSelect = onDoneSelect,
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessage(
    message: UiMessage,
    selected: Boolean,
    selecting: Boolean,
    onLongPress: (UiMessage) -> Unit,
    onDoneSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(
                        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                        else Modifier
                    )
                    .combinedClickable(
                        enabled = !selecting,
                        onClick = {},
                        onLongClick = { onLongPress(message) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val body: @Composable () -> Unit = {
                    Column {
                        if (message.attachmentPath != null) {
                            AsyncImage(
                                model = File(message.attachmentPath),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .heightIn(max = 260.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            if (message.content.isNotBlank()) Spacer(Modifier.height(8.dp))
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                if (selecting) SelectionContainer { body() } else body()
            }
            if (selecting) {
                Spacer(Modifier.height(6.dp))
                DoneChip(onDoneSelect)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantMessage(
    message: UiMessage,
    streaming: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onLongPress: (UiMessage) -> Unit,
    onCopy: (UiMessage) -> Unit,
    onRegenerate: (UiMessage) -> Unit,
    onDoneSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(2.dp, scheme.primary, shape)
                else Modifier
            )
            .combinedClickable(
                enabled = !selecting,
                onClick = {},
                onLongClick = { onLongPress(message) },
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MangosteenAvatar()
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Mangosteen",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.primary,
            )
        }

        if (!message.reasoning.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            ReasoningCard(message)
        }

        Spacer(Modifier.height(10.dp))
        if (selecting) {
            SelectionContainer {
                MarkdownText(
                    text = message.content,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (message.content.isEmpty() && streaming) {
            Text(
                text = "Thinking…",
                fontSize = 16.sp,
                color = scheme.onSurfaceVariant,
            )
        } else {
            MarkdownText(
                text = message.content,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!streaming && message.id > 0L) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MessageAction("Copy", Icons.Rounded.ContentCopy) { onCopy(message) }
                MessageAction("Regenerate", Icons.Rounded.Refresh) { onRegenerate(message) }
            }
        }
        if (selecting) {
            Spacer(Modifier.height(6.dp))
            DoneChip(onDoneSelect)
        }
    }
}

@Composable
private fun ReasoningCard(message: UiMessage) {
    var expanded by remember(message) { mutableStateOf(false) }
    val summary = remember(message.reasoning) {
        message.reasoning.orEmpty()
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { expanded = !expanded }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    // shortcut: the API does not report reasoning time, label it generically
                    text = "Thought for 8s",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    if (expanded) {
                        Text(
                            text = message.reasoning.orEmpty(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = summary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Hide reasoning" else "Show reasoning",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MessageAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DoneChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Done",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ---------------------------------------------------------------- composer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerArea(
    input: String,
    onInputChange: (String) -> Unit,
    focusRequester: FocusRequester,
    editing: Boolean,
    onCancelEdit: () -> Unit,
    attachmentPath: String?,
    onRemoveAttachment: () -> Unit,
    streaming: Boolean,
    reasoning: Reasoning,
    searchOn: Boolean,
    onReasoningClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onPickMedia: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        if (editing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .background(scheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Editing message",
                    fontSize = 12.sp,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel edit",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onCancelEdit)
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }

        if (attachmentPath != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = File(attachmentPath),
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Image ready to send",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove image",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRemoveAttachment)
                        .padding(6.dp)
                        .size(18.dp),
                )
            }
        }

        Text(
            text = "Mangosteen can make mistakes. Check important info.",
            fontSize = 11.sp,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        val composerShape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
                .clip(composerShape)
                .background(scheme.surfaceContainerLow)
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), composerShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        color = scheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    ),
                    cursorBrush = SolidColor(scheme.primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box {
                            if (input.isEmpty()) {
                                Text(
                                    text = "Message Mangosteen...",
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .clickable(onClick = onPickMedia),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Attach image",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ThinkPill(on = reasoning != Reasoning.OFF, onClick = onReasoningClick)
                Spacer(Modifier.width(8.dp))
                SearchPill(on = searchOn, onClick = onToggleSearch)
                Spacer(Modifier.weight(1f))
                val canSend = streaming || input.isNotBlank() || attachmentPath != null
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) scheme.primary else scheme.surfaceContainerHigh
                        )
                        .clickable(enabled = canSend) {
                            if (streaming) onStop() else onSend()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (streaming) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                        contentDescription = if (streaming) "Stop" else "Send",
                        tint = if (canSend) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkPill(on: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = if (on) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Think",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SearchPill(on: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (on) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        val tint = if (on) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Search",
            fontSize = 12.sp,
            color = tint,
        )
    }
}

// ---------------------------------------------------------- slash commands

@Composable
private fun CommandMenu(
    commands: List<SlashCommand>,
    onPick: (SlashCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Commands",
            fontSize = 12.sp,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        commands.forEachIndexed { index, command ->
            val selected = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) scheme.surfaceContainerHigh else Color.Transparent)
                    .clickable { onPick(command) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = command.icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = command.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = command.hint,
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardReturn,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    message: UiMessage,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            Text(
                text = if (message.isUser) "You" else "Mangosteen",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message.content.ifBlank {
                    if (message.attachmentPath != null) "Image" else ""
                },
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            SoftDivider()
            Spacer(Modifier.height(8.dp))
            SheetActionRow("Copy", Icons.Rounded.ContentCopy, onCopy)
            SheetActionRow("Edit", Icons.Rounded.Edit, onEdit)
            if (!message.isUser) {
                SheetActionRow("Regenerate", Icons.Rounded.Refresh, onRegenerate)
            }
            SheetActionRow("Select text", Icons.Rounded.SelectAll, onSelect)
            Spacer(Modifier.height(4.dp))
            SoftDivider()
            Spacer(Modifier.height(4.dp))
            SheetActionRow("Delete", Icons.Rounded.DeleteOutline, onDelete, tint = scheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onSystemPrompt: () -> Unit,
    onCheckInHour: () -> Unit,
    onCheckInTomorrow: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            SheetHeading(
                title = "Chat",
                subtitle = "Rename, schedule a check-in, or start over.",
            )
            Spacer(Modifier.height(16.dp))
            SheetActionRow("Rename", Icons.Rounded.Edit, onRename)
            SheetActionRow("System prompt", Icons.Rounded.Tune, onSystemPrompt)
            SoftDivider()
            Spacer(Modifier.height(4.dp))
            SheetActionRow("Check in 1 hour", Icons.Rounded.Schedule, onCheckInHour)
            SheetActionRow("Check in tomorrow 9am", Icons.Rounded.Alarm, onCheckInTomorrow)
            Spacer(Modifier.height(4.dp))
            SoftDivider()
            Spacer(Modifier.height(4.dp))
            SheetActionRow("Clear messages", Icons.Rounded.DeleteSweep, onClear)
            SheetActionRow(
                "Delete chat",
                Icons.Rounded.DeleteOutline,
                onDelete,
                tint = scheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasoningSheet(
    selected: Reasoning,
    onChoose: (Reasoning) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val options = listOf(
        Reasoning.ON to ("On" to "Balanced thinking for everyday answers"),
        Reasoning.LOW to ("Low" to "Quick answers with light reasoning"),
        Reasoning.HIGH to ("High" to "Deeper thinking for hard problems"),
        Reasoning.OFF to ("Off" to "Reply straight away"),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            SheetHeading(
                title = "Thinking",
                subtitle = "How much time Mangosteen spends before replying.",
            )
            Spacer(Modifier.height(16.dp))
            options.forEach { (level, labels) ->
                val isSelected = level == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) scheme.surfaceContainer else Color.Transparent
                        )
                        .clickable { onChoose(level) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = labels.first,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = labels.second,
                            fontSize = 14.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = scheme.primary,
                            unselectedColor = scheme.outline,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ----------------------------------------------------------------- dialogs

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    singleLine: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(12.dp),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(
                        text = "Optional",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

// ----------------------------------------------------------------- helpers

private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Mangosteen", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun separatorNeeded(previous: Long, current: Long): Boolean {
    if (current - previous > 30 * 60 * 1000L) return true
    val a = Calendar.getInstance().apply { timeInMillis = previous }
    val b = Calendar.getInstance().apply { timeInMillis = current }
    return a.get(Calendar.YEAR) != b.get(Calendar.YEAR) ||
        a.get(Calendar.DAY_OF_YEAR) != b.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(time: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = time }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    if (sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return "Today"
    }
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameYear && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }
    val pattern = if (sameYear) "MMM d" else "MMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))
}
