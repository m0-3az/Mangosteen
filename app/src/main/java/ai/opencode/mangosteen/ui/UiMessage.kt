package ai.opencode.mangosteen.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Mutable row model for the chat list. Hot fields are Compose state so that
 * streaming updates to the last bubble recompose without rebuilding the list.
 */
class UiMessage(
    var id: Long,
    val role: String,
    content: String,
    reasoning: String?,
    var createdAt: Long = System.currentTimeMillis(),
    promptTokens: Int? = null,
    completionTokens: Int? = null,
    val attachmentPath: String? = null,
    val attachmentMime: String? = null,
    reasoningMillis: Long? = null,
) {
    var content by mutableStateOf(content)
    var reasoning by mutableStateOf(reasoning)
    var promptTokens by mutableStateOf(promptTokens)
    var completionTokens by mutableStateOf(completionTokens)
    var reasoningMillis by mutableStateOf(reasoningMillis)

    val isUser: Boolean get() = role == "user"
}

/**
 * Keeps the first [index] messages and returns them; drops message [index] and
 * everything after it. Used when an edited or regenerated message invalidates
 * the rest of the thread.
 */
internal fun MutableList<UiMessage>.purgeFrom(index: Int): List<UiMessage> {
    val kept = take(index)
    while (size > index) removeAt(size - 1)
    return kept
}
