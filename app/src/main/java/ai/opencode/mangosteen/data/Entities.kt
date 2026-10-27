package ai.opencode.mangosteen.data

data class Conversation(
    val id: Long,
    val title: String,
    val sessionId: String,
    val modelId: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSeenAt: Long = 0L
)

/** A conversation plus a preview of its newest message, for the list screen. */
data class ConversationRow(
    val conversation: Conversation,
    val lastMessage: String?,
    val lastRole: String?,
    val unreadCount: Int = 0
)

/**
 * A follow-up the assistant will send on its own. [note] is the private cue
 * used to compose the check-in, not shown to the user.
 */
data class Reminder(
    val id: Long,
    val conversationId: Long,
    val fireAt: Long,
    val note: String,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val role: String,
    val content: String,
    val reasoning: String?,
    val modelId: String?,
    val createdAt: Long,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    /** Local file holding an attached image, if the turn had one. */
    val attachmentPath: String? = null,
    val attachmentMime: String? = null,
    /** How long the visible reasoning stream lasted, in milliseconds. */
    val reasoningMillis: Long? = null
)
