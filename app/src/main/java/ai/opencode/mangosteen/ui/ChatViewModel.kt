package ai.opencode.mangosteen.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.data.Conversation
import ai.opencode.mangosteen.data.Message
import ai.opencode.mangosteen.net.ChatMessage
import ai.opencode.mangosteen.net.GoApiClient
import ai.opencode.mangosteen.net.ModelCatalog
import ai.opencode.mangosteen.net.ModelInfo
import ai.opencode.mangosteen.data.Reminder
import ai.opencode.mangosteen.net.Reasoning
import ai.opencode.mangosteen.net.RequestOptions
import ai.opencode.mangosteen.proactive.ProactivePrompt
import ai.opencode.mangosteen.proactive.ProactiveScheduler
import ai.opencode.mangosteen.proactive.ScheduleParser
import ai.opencode.mangosteen.settings.SecurePrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class Attachment(val path: String, val mime: String)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val NEW_TITLE = "New chat"
    }

    private val repo = ChatRepository(app)
    private val prefs = SecurePrefs(app)
    private val ui = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var conversation by mutableStateOf<Conversation?>(null)

    val messages = mutableStateListOf<UiMessage>()

    var streaming by mutableStateOf(false)
    var editing by mutableStateOf<UiMessage?>(null)
    var pendingAttachment by mutableStateOf<Attachment?>(null)
    var reasoning by mutableStateOf(Reasoning.ON)
    var search by mutableStateOf(false)

    var toast by mutableStateOf<String?>(null)
    var contextText by mutableStateOf("No usage yet")
    var contextProgress by mutableStateOf(0f)

    var modelOptions by mutableStateOf<List<ModelInfo>>(emptyList())
    var loadingModels by mutableStateOf(false)

    private var client: GoApiClient? = null
    private var job: Job? = null

    // --------------------------------------------------------------- loading

    fun load(id: Long) {
        if (conversation?.id == id && messages.isNotEmpty()) return
        viewModelScope.launch {
            val conv = withContext(Dispatchers.IO) { repo.conversation(id) } ?: return@launch
            bind(conv)
            val loaded = withContext(Dispatchers.IO) { repo.messages(id) }
            messages.clear()
            messages.addAll(
                loaded.map {
                    UiMessage(
                        it.id, it.role, it.content, it.reasoning, it.createdAt,
                        it.promptTokens, it.completionTokens,
                        it.attachmentPath, it.attachmentMime, it.reasoningMillis,
                    )
                }
            )
            withContext(Dispatchers.IO) { repo.markSeen(id) }
            renderChips()
            updateStatus()
        }
    }

    private fun bind(conv: Conversation) {
        conversation = conv
    }

    fun searchAvailable(): Boolean {
        val conv = conversation ?: return false
        return ModelCatalog.searchSupported(conv.modelId)
    }

    // -------------------------------------------------------------- toggles

    fun chooseReasoning(level: Reasoning) {
        prefs.setReasoningLevel(level.name.lowercase())
        renderChips()
    }

    private fun currentReasoning(): Reasoning = when (prefs.reasoningLevel()) {
        "off" -> Reasoning.OFF
        "low" -> Reasoning.LOW
        "high" -> Reasoning.HIGH
        else -> Reasoning.ON
    }

    fun toggleSearch() {
        if (!searchAvailable()) {
            toast = "Search isn't available for this model"
            return
        }
        prefs.setSearchEnabled(!prefs.searchEnabled())
        renderChips()
    }

    private fun renderChips() {
        reasoning = currentReasoning()
        search = prefs.searchEnabled() && searchAvailable()
    }

    // -------------------------------------------------------------- editing

    fun startEdit(message: UiMessage) {
        if (streaming) {
            toast = "Wait for the current reply to finish"
            return
        }
        editing = message
    }

    fun cancelEdit() {
        editing = null
    }

    /** Saves an edited assistant reply in place; later turns build on it. */
    fun editAssistant(message: UiMessage, text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        message.content = cleaned
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.updateMessageContent(message.id, cleaned) }
        }
    }

    fun attachImage(uri: android.net.Uri) {
        val conv = conversation ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { ImageStore.save(getApplication(), uri) }
            if (saved == null) {
                toast = "Couldn't read that image"
                return@launch
            }
            pendingAttachment = Attachment(saved.first, saved.second)
            if (!ModelCatalog.visionSupported(conv.modelId)) {
                toast = "${ModelCatalog.prettify(conv.modelId)} may not be able to read images"
            }
        }
    }

    fun clearAttachment() {
        pendingAttachment = null
    }

    // -------------------------------------------------------------- sending

    fun send(text: String) {
        val conv = conversation ?: return
        val trimmed = text.trim()
        val editingMessage = editing
        // An edit keeps the original image unless a new one is attached.
        val attachment = pendingAttachment ?: editingMessage?.let { edited ->
            edited.attachmentPath?.let { Attachment(it, edited.attachmentMime ?: "image/jpeg") }
        }
        if ((trimmed.isEmpty() && attachment == null) || streaming) return

        val key = prefs.apiKey()
        if (key.isNullOrBlank()) {
            toast = "Add your API key in Settings first."
            return
        }

        val now = System.currentTimeMillis()
        editing = null
        if (editingMessage != null) {
            val index = messages.indexOfFirst { it === editingMessage }
            if (index >= 0) messages.purgeFrom(index)
        }

        pendingAttachment = null

        val userMessage = UiMessage(
            0L, "user", trimmed, null, now,
            attachmentPath = attachment?.path,
            attachmentMime = attachment?.mime,
        )
        messages.add(userMessage)

        val historySnapshot = messages.toList()

        viewModelScope.launch {
            val userId = withContext(Dispatchers.IO) {
                if (editingMessage != null && editingMessage.id > 0L) {
                    repo.deleteMessagesFrom(conv.id, editingMessage.id)
                }
                repo.addMessage(
                    Message(
                        0L, conv.id, "user", trimmed, null, null, now,
                        attachmentPath = attachment?.path,
                        attachmentMime = attachment?.mime,
                    )
                )
            }
            userMessage.id = userId
            if (conv.title == NEW_TITLE) {
                val renamed = conv.copy(
                    title = trimmed.ifBlank { "Image" }.take(48),
                    updatedAt = now,
                )
                conversation = renamed
                withContext(Dispatchers.IO) { repo.updateConversation(renamed) }
            } else {
                withContext(Dispatchers.IO) { repo.touch(conv.id, now) }
            }
        }

        launchReply(conv, key, historySnapshot)
    }

    /** Wipes [message] and everything after it from screen and storage, then streams a fresh reply. */
    fun regenerate(message: UiMessage) {
        val conv = conversation ?: return
        if (streaming || message.isUser || message.id <= 0L) return
        val key = prefs.apiKey()
        if (key.isNullOrBlank()) {
            toast = "Add your API key in Settings first."
            return
        }
        val index = messages.indexOfFirst { it === message }
        if (index < 0) return
        val history = messages.purgeFrom(index)
        updateStatus()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteMessagesFrom(conv.id, message.id) }
            launchReply(conv, key, history)
        }
    }

    /** Removes [message] and everything after it, from the screen and storage. */
    fun deleteFrom(message: UiMessage) {
        val conv = conversation ?: return
        if (streaming) {
            toast = "Wait for the current reply to finish"
            return
        }
        val index = messages.indexOfFirst { it === message }
        if (index < 0) return
        messages.purgeFrom(index)
        updateStatus()
        if (message.id > 0L) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repo.deleteMessagesFrom(conv.id, message.id) }
            }
        }
    }

    /** Streams a reply for [historySnapshot] and appends it as the last message. */
    private fun launchReply(conv: Conversation, key: String, historySnapshot: List<UiMessage>) {
        val model = ModelCatalog.info(conv.modelId)
        val options = RequestOptions(
            reasoning = currentReasoning(),
            search = prefs.searchEnabled() && searchAvailable(),
        )
        val streamingMessage = UiMessage(-1L, "assistant", "", null, System.currentTimeMillis())
        messages.add(streamingMessage)
        streaming = true

        val api = GoApiClient(key)
        client = api
        val reasoningStartedAt = AtomicLong(0L)
        val reasoningFinishedAt = AtomicLong(0L)

        job = viewModelScope.launch {
            fun finishThinking() {
                if (streamingMessage.reasoningMillis != null) return
                val started = reasoningStartedAt.get()
                if (started <= 0L) return
                val ended = reasoningFinishedAt.get()
                    .takeIf { it >= started }
                    ?: SystemClock.elapsedRealtime()
                streamingMessage.reasoningMillis = (ended - started).coerceAtLeast(0L)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    val history = historySnapshot
                        .filter { it.content.isNotEmpty() || it.attachmentPath != null }
                        .map { message ->
                            ChatMessage(
                                role = message.role,
                                content = message.content,
                                imageBase64 = message.attachmentPath?.let { ImageStore.base64(it) },
                                imageMime = message.attachmentMime,
                            )
                        }
                    api.stream(
                        model, buildSystemPrompt(conv.systemPrompt), history, conv.sessionId, options,
                        onNotice = { notice -> ui.launch { toast = notice } },
                    ) { content, reasoning ->
                        if (reasoning.isNotEmpty()) {
                            val now = SystemClock.elapsedRealtime()
                            reasoningStartedAt.compareAndSet(0L, now)
                            reasoningFinishedAt.set(now)
                        }
                        ui.launch {
                            if (content.isNotEmpty()) streamingMessage.content += content
                            if (reasoning.isNotEmpty()) {
                                streamingMessage.reasoning =
                                    (streamingMessage.reasoning ?: "") + reasoning
                            }
                        }
                    }
                }
                finishThinking()
                val (clean, schedules) = ScheduleParser.extract(result.content)
                if (schedules.isNotEmpty()) {
                    streamingMessage.content = clean
                    persistAssistant(conv, streamingMessage, result.copy(content = clean))
                    queueReminders(conv, schedules)
                } else {
                    persistAssistant(conv, streamingMessage, result)
                }
                maybeAutoTitle(conv, historySnapshot)
            } catch (cancel: CancellationException) {
                finishThinking()
                persistAssistant(
                    conv, streamingMessage,
                    ChatMessage("assistant", streamingMessage.content, streamingMessage.reasoning),
                )
                throw cancel
            } catch (error: Exception) {
                finishThinking()
                val detail = error.message ?: "Error"
                streamingMessage.content = if (streamingMessage.content.isEmpty()) {
                    "⚠ $detail"
                } else {
                    streamingMessage.content + "\n\n⚠ $detail"
                }
                persistAssistant(
                    conv, streamingMessage,
                    ChatMessage("assistant", streamingMessage.content, streamingMessage.reasoning),
                )
            } finally {
                streaming = false
                client = null
                updateStatus()
            }
        }
    }

    private suspend fun persistAssistant(conv: Conversation, uiMessage: UiMessage, result: ChatMessage) {
        if (result.content.isBlank() && result.reasoning.isNullOrBlank()) return
        uiMessage.promptTokens = result.usage?.promptTokens
        uiMessage.completionTokens = result.usage?.completionTokens
        val id = withContext(NonCancellable + Dispatchers.IO) {
            repo.addMessage(
                Message(
                    0L, conv.id, "assistant", result.content, result.reasoning,
                    conv.modelId, System.currentTimeMillis(),
                    result.usage?.promptTokens, result.usage?.completionTokens,
                    reasoningMillis = uiMessage.reasoningMillis,
                )
            ).also { repo.touch(conv.id) }
        }
        uiMessage.id = id
    }

    /**
     * After the opening exchange, asks the chat's own model for a short title
     * and swaps it in — unless the user renamed the chat meanwhile or the
     * reply never landed.
     */
    private fun maybeAutoTitle(conv: Conversation, history: List<UiMessage>) {
        val first = history.firstOrNull { it.isUser }?.content ?: return
        if (first.isBlank()) return
        if (messages.size != 2) return
        val reply = messages.lastOrNull() ?: return
        if (reply.isUser || reply.id <= 0L) return
        val placeholder = first.ifBlank { "Image" }.take(48)
        if (conversation?.title != placeholder) return
        val key = prefs.apiKey() ?: return

        val model = ModelCatalog.info(conv.modelId)
        val replyText = reply.content
        viewModelScope.launch {
            val raw = runCatching {
                withContext(Dispatchers.IO) {
                    GoApiClient(key).stream(
                        model,
                        ChatTitle.INSTRUCTION,
                        listOf(ChatMessage("user", ChatTitle.request(first, replyText))),
                        conv.sessionId,
                        RequestOptions(reasoning = Reasoning.OFF, search = false),
                    ) { _, _ -> }.content
                }
            }.getOrNull() ?: return@launch
            val title = ChatTitle.clean(raw) ?: return@launch
            val current = conversation ?: return@launch
            if (current.id != conv.id || current.title != placeholder) return@launch
            val updated = current.copy(title = title, updatedAt = System.currentTimeMillis())
            conversation = updated
            withContext(Dispatchers.IO) { repo.updateConversation(updated) }
        }
    }

    fun stop() {
        client?.cancel()
        job?.cancel()
    }

    // ---------------------------------------------------------- context stats

    private fun updateStatus() {
        val conv = conversation ?: return
        val max = ModelCatalog.contextWindow(conv.modelId)
        var contextUsed = 0
        var spent = 0L
        for (m in messages) {
            val p = m.promptTokens ?: 0
            val c = m.completionTokens ?: 0
            if (p > 0 || c > 0) spent += (p + c).toLong()
            if (!m.isUser && p > contextUsed) contextUsed = p
        }
        if (contextUsed <= 0) {
            contextText = "No usage yet"
            contextProgress = 0f
            return
        }
        val pct = (contextUsed * 100.0 / max).coerceIn(0.0, 100.0)
        contextText = "${formatCount(contextUsed.toLong())} / ${formatCount(max.toLong())}" +
            " (${pct.toInt()}%) · ${formatCount(spent)} spent"
        contextProgress = (pct / 100.0).toFloat()
    }

    // ---------------------------------------------------------------- models

    fun loadModels() {
        if (loadingModels) return
        if (modelOptions.isEmpty()) {
            modelOptions = ModelCatalog.sorted(ModelCatalog.FALLBACK)
        }
        loadingModels = true
        val key = prefs.apiKey()
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                if (key.isNullOrBlank()) null
                else runCatching { GoApiClient(key).listModels() }.getOrNull()
            }
            modelOptions = ModelCatalog.sorted(ids ?: ModelCatalog.FALLBACK)
            loadingModels = false
        }
    }

    fun applyModel(model: ModelInfo) {
        val conv = conversation ?: return
        val updated = conv.copy(modelId = model.id, updatedAt = System.currentTimeMillis())
        conversation = updated
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.updateConversation(updated) } }
        renderChips()
        updateStatus()
    }

    // -------------------------------------------------------------- overflow

    fun rename(title: String) {
        val conv = conversation ?: return
        val updated = conv.copy(
            title = title.trim().ifBlank { NEW_TITLE },
            updatedAt = System.currentTimeMillis(),
        )
        conversation = updated
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.updateConversation(updated) } }
    }

    fun setSystemPrompt(prompt: String) {
        val conv = conversation ?: return
        val updated = conv.copy(
            systemPrompt = prompt.trim().ifBlank { null },
            updatedAt = System.currentTimeMillis(),
        )
        conversation = updated
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.updateConversation(updated) } }
    }

    fun clearMessages() {
        val conv = conversation ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.clearMessages(conv.id) }
            messages.clear()
            updateStatus()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val conv = conversation ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteConversation(conv.id) }
            onDeleted()
        }
    }

    // ------------------------------------------------------- proactive check-ins

    /** Global prompt first, then the chat's own one, then the built-in check-in rules. */
    private fun buildSystemPrompt(userPrompt: String?): String {
        val global = prefs.defaultSystemPrompt().trim()
        return listOfNotNull(
            global.ifBlank { null },
            userPrompt?.takeIf { it.isNotBlank() && it.trim() != global },
            ProactivePrompt.instruction().takeIf { prefs.proactiveEnabled() },
        ).joinToString("\n\n")
    }

    private fun queueReminders(conv: Conversation, schedules: List<ScheduleParser.Schedule>) {
        viewModelScope.launch {
            for (schedule in schedules) {
                val id = withContext(Dispatchers.IO) {
                    repo.addReminder(conv.id, schedule.fireAt, schedule.note)
                }
                ProactiveScheduler.schedule(
                    getApplication(),
                    Reminder(id, conv.id, schedule.fireAt, schedule.note),
                )
            }
        }
    }

    /** Manual path, so a check-in can be queued even if the model didn't offer one. */
    fun scheduleCheckIn(fireAt: Long, note: String = "check in on the user") {
        val conv = conversation ?: return
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { repo.addReminder(conv.id, fireAt, note) }
            ProactiveScheduler.schedule(getApplication(), Reminder(id, conv.id, fireAt, note))
            val whenText = SimpleDateFormat("h:mma", Locale.getDefault()).format(Date(fireAt))
            toast = "Check-in scheduled for $whenText"
        }
    }

    fun inOneHour(): Long = System.currentTimeMillis() + 60L * 60L * 1000L

    fun tomorrowMorning(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    override fun onCleared() {
        client?.cancel()
        ui.cancel()
        repo.close()
        super.onCleared()
    }
}
