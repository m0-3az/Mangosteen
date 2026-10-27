package ai.opencode.mangosteen.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.opencode.mangosteen.data.ConversationRow
import ai.opencode.mangosteen.ui.components.FilterPill
import ai.opencode.mangosteen.ui.components.HeroMark
import ai.opencode.mangosteen.ui.components.MangosteenBackground
import ai.opencode.mangosteen.ui.components.PrimaryButton
import ai.opencode.mangosteen.ui.components.RoundIconButton
import ai.opencode.mangosteen.ui.components.SectionHeader
import ai.opencode.mangosteen.ui.components.SheetActionRow
import ai.opencode.mangosteen.ui.components.SheetHeading
import java.util.Calendar

private enum class ChatFilter(val label: String) {
    ALL("All"), FAVORITES("Favorites")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onOpen: (Long) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatsViewModel = viewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ChatFilter.ALL) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var sheetRow by remember { mutableStateOf<ConversationRow?>(null) }
    var renameRow by remember { mutableStateOf<ConversationRow?>(null) }
    var deleteRow by remember { mutableStateOf<ConversationRow?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    val visible = remember(rows, filter, favorites, query) {
        val base = rows.filter { row ->
            when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.FAVORITES -> favorites.contains(row.conversation.id)
            }
        }
        if (query.isBlank()) base
        else base.filter {
            it.conversation.title.contains(query, ignoreCase = true) ||
                it.lastMessage?.contains(query, ignoreCase = true) == true
        }
    }

    fun toggleSelected(id: Long) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun leaveSelection() {
        selecting = false
        selected = emptySet()
    }

    val groups = remember(visible) {
        val now = Calendar.getInstance()
        val todayStart = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val recent = visible.filter { it.conversation.updatedAt >= todayStart }
        val yesterday = visible.filter {
            it.conversation.updatedAt >= yesterdayStart && it.conversation.updatedAt < todayStart
        }
        val earlier = visible.filter { it.conversation.updatedAt < yesterdayStart }
        listOf("Recent" to recent, "Yesterday" to yesterday, "Earlier" to earlier)
    }

    MangosteenBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Chats",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (rows.size == 1) "1 conversation" else "${rows.size} conversations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RoundIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = { searchFocus.requestFocus() },
                    )
                    Spacer(Modifier.width(10.dp))
                    RoundIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search chats and messages",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ChatFilter.entries.toList(), key = { it.name }) { item ->
                        FilterPill(
                            label = item.label,
                            selected = filter == item,
                            onClick = { filter = item },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    rows.isEmpty() && query.isBlank() && filter == ChatFilter.ALL -> EmptyChats(onNewChat)
                    visible.isEmpty() -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (filter) {
                                ChatFilter.FAVORITES -> "No favorite chats yet"
                                ChatFilter.ALL -> "No chats match your search"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            SectionHeader(
                                title = "Recent",
                                action = if (selecting) "Done" else "Select",
                                onAction = {
                                    if (selecting) leaveSelection() else selecting = true
                                },
                            )
                        }
                        if (selecting && selected.isNotEmpty()) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${selected.size} selected",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = {
                                        selected.filterNot { favorites.contains(it) }
                                            .forEach { viewModel.toggleFavorite(it) }
                                        selected = emptySet()
                                    }) {
                                        Text("Pin", color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { confirmBulkDelete = true }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        groups.forEach { (label, groupRows) ->
                            if (groupRows.isEmpty()) return@forEach
                            if (label != "Recent") {
                                item {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                            items(groupRows, key = { it.conversation.id }) { row ->
                                ConversationCard(
                                    row = row,
                                    selecting = selecting,
                                    selected = selected.contains(row.conversation.id),
                                    onClick = {
                                        if (selecting) toggleSelected(row.conversation.id)
                                        else onOpen(row.conversation.id)
                                    },
                                    onLongClick = {
                                        if (selecting) toggleSelected(row.conversation.id)
                                        else sheetRow = row
                                    },
                                )
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New chat") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp),
            )
        }
    }

    sheetRow?.let { row ->
        val pinned = favorites.contains(row.conversation.id)
        ModalBottomSheet(
            onDismissRequest = { sheetRow = null },
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
                SheetHeading(title = row.conversation.title, subtitle = "Chat options")
                Spacer(Modifier.height(16.dp))
                SheetActionRow(
                    label = "Rename",
                    icon = Icons.Rounded.Edit,
                    onClick = {
                        sheetRow = null
                        renameRow = row
                    },
                )
                SheetActionRow(
                    label = if (pinned) "Unpin" else "Pin",
                    icon = Icons.Rounded.PushPin,
                    onClick = {
                        viewModel.toggleFavorite(row.conversation.id)
                        sheetRow = null
                    },
                )
                SheetActionRow(
                    label = "Delete",
                    icon = Icons.Rounded.Delete,
                    onClick = {
                        sheetRow = null
                        deleteRow = row
                    },
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    renameRow?.let { row ->
        var title by remember(row.conversation.id) {
            mutableStateOf(row.conversation.title)
        }
        AlertDialog(
            onDismissRequest = { renameRow = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Rename chat",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isNotEmpty()) viewModel.rename(row.conversation, trimmed)
                        renameRow = null
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameRow = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    deleteRow?.let { row ->
        AlertDialog(
            onDismissRequest = { deleteRow = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Delete chat?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "\"${row.conversation.title}\" and its messages will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(row.conversation)
                    deleteRow = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRow = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Delete ${selected.size} chats?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "Their messages and reminders will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    rows.filter { selected.contains(it.conversation.id) }
                        .forEach { viewModel.delete(it.conversation) }
                    confirmBulkDelete = false
                    leaveSelection()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun EmptyChats(onNewChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HeroMark()
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Start a conversation and it will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "New chat",
            onClick = onNewChat,
            modifier = Modifier.width(200.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationCard(
    row: ConversationRow,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = 2.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = row.conversation.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.lastMessage?.let { plainText(it) }.orEmpty().ifBlank { "No messages yet" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = relativeTime(row.conversation.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.width(60.dp),
        )
    }
}
