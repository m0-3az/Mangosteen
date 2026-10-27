package ai.opencode.mangosteen.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.data.Conversation
import ai.opencode.mangosteen.net.ChatMessage
import ai.opencode.mangosteen.net.GoApiClient
import ai.opencode.mangosteen.net.ModelCatalog
import ai.opencode.mangosteen.net.ModelInfo
import ai.opencode.mangosteen.net.Reasoning
import ai.opencode.mangosteen.net.RequestOptions
import ai.opencode.mangosteen.settings.SecurePrefs
import ai.opencode.mangosteen.settings.VoiceExtraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SecurePrefs(app)
    private val repo = ChatRepository(app)

    var apiKey by mutableStateOf(prefs.apiKey().orEmpty())
    var modelId by mutableStateOf(prefs.defaultModel() ?: ModelCatalog.DEFAULT_MODEL)
    var systemPrompt by mutableStateOf(prefs.defaultSystemPrompt())
    var proactive by mutableStateOf(prefs.proactiveEnabled())
    var notifications by mutableStateOf(prefs.notificationsEnabled())

    var modelOptions by mutableStateOf<List<ModelInfo>>(emptyList())
    var loadingModels by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)

    var chats by mutableStateOf<List<Conversation>>(emptyList())
    var extracting by mutableStateOf(false)

    fun modelLabel(): String = ModelCatalog.prettify(modelId)

    fun loadModels() {
        if (loadingModels) return
        loadingModels = true
        if (modelOptions.isEmpty()) modelOptions = ModelCatalog.sorted(ModelCatalog.FALLBACK)
        val typed = apiKey.trim()
        val key = typed.ifBlank { prefs.apiKey() }
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                if (key.isNullOrBlank()) null
                else runCatching { GoApiClient(key).listModels() }.getOrNull()
            }
            modelOptions = ModelCatalog.sorted(ids ?: ModelCatalog.FALLBACK)
            loadingModels = false
        }
    }

    fun save(onDone: () -> Unit) {
        prefs.setApiKey(apiKey.trim().ifBlank { null })
        prefs.setDefaultModel(modelId)
        prefs.setDefaultSystemPrompt(systemPrompt)
        prefs.setProactiveEnabled(proactive)
        prefs.setNotificationsEnabled(notifications)
        toast = "Saved"
        onDone()
    }

    /**
     * Persists the persona prompt the moment the dialog is confirmed. Waiting
     * for the screen-level Save button meant a user who pressed the dialog's
     * Save and then backed out lost the prompt entirely.
     */
    fun saveSystemPrompt(value: String) {
        systemPrompt = value
        prefs.setDefaultSystemPrompt(value)
        toast = "System prompt saved"
    }

    fun clearAllChats() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteAllConversations() }
            chats = withContext(Dispatchers.IO) { repo.conversations() }
            toast = "All chats cleared"
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            chats = withContext(Dispatchers.IO) { repo.conversations() }
        }
    }

    /**
     * Runs the selected chat's replies through the character extraction prompt
     * on that chat's own model and fills the global prompt field with the result.
     */
    fun extractVoice(conv: Conversation) {
        if (extracting) return
        val key = apiKey.trim().ifBlank { prefs.apiKey().orEmpty() }
        if (key.isBlank()) {
            toast = "Add your API key first"
            return
        }
        extracting = true
        viewModelScope.launch {
            val samples = withContext(Dispatchers.IO) { VoiceExtraction.samples(repo.messages(conv.id)) }
            if (samples.isBlank()) {
                extracting = false
                toast = "That chat has no replies to learn from yet"
                return@launch
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    GoApiClient(key).stream(
                        ModelCatalog.info(conv.modelId),
                        VoiceExtraction.INSTRUCTION,
                        listOf(ChatMessage("user", samples)),
                        conv.sessionId,
                        RequestOptions(reasoning = Reasoning.OFF, search = false),
                    ) { _, _ -> }.content.trim()
                }
            }
            extracting = false
            val text = result.getOrNull()
            if (!text.isNullOrBlank()) {
                systemPrompt = text
                toast = "Character extracted — review it, then Save"
            } else {
                toast = "Extraction failed: ${result.exceptionOrNull()?.message ?: "empty reply"}"
            }
        }
    }

    override fun onCleared() {
        repo.close()
        super.onCleared()
    }
}
