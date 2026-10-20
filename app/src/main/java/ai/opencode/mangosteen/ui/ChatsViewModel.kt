package ai.opencode.mangosteen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.data.Conversation
import ai.opencode.mangosteen.data.ConversationRow
import ai.opencode.mangosteen.net.ModelCatalog
import ai.opencode.mangosteen.settings.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)
    private val prefs = SecurePrefs(app)

    private val _rows = MutableStateFlow<List<ConversationRow>>(emptyList())
    val rows: StateFlow<List<ConversationRow>> = _rows.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    fun refresh() {
        _favorites.value = prefs.favorites()
        viewModelScope.launch {
            _rows.value = withContext(Dispatchers.IO) { repo.conversationRows() }
        }
    }

    fun hasApiKey(): Boolean = !prefs.apiKey().isNullOrBlank()

    fun defaultModelLabel(): String = ModelCatalog.prettify(prefs.defaultModel() ?: ModelCatalog.DEFAULT_MODEL)

    fun toggleFavorite(id: Long) {
        prefs.toggleFavorite(id)
        _favorites.value = prefs.favorites()
    }

    fun delete(conversation: Conversation, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteConversation(conversation.id) }
            _rows.value = withContext(Dispatchers.IO) { repo.conversationRows() }
            onDone()
        }
    }

    /** Re-registers persisted reminders (also covers the "after reboot" case). */
    fun rescheduleReminders() {
        viewModelScope.launch {
            val pending = withContext(Dispatchers.IO) { repo.pendingReminders() }
            for (reminder in pending) {
                ai.opencode.mangosteen.proactive.ProactiveScheduler.schedule(
                    getApplication(),
                    reminder,
                )
            }
        }
    }

    fun createChat(onReady: (Long) -> Unit) {
        val model = prefs.defaultModel() ?: ModelCatalog.DEFAULT_MODEL
        viewModelScope.launch {
            // The global system prompt is merged in at request time, not stored per chat.
            val conversation = withContext(Dispatchers.IO) {
                repo.createConversation(model, null)
            }
            onReady(conversation.id)
        }
    }

    fun rename(conversation: Conversation, title: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateConversation(conversation.copy(title = title))
            }
            _rows.value = withContext(Dispatchers.IO) { repo.conversationRows() }
        }
    }

    override fun onCleared() {
        repo.close()
        super.onCleared()
    }
}
