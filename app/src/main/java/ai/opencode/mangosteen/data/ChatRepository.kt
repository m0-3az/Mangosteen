package ai.opencode.mangosteen.data

import android.content.ContentValues
import android.content.Context
import java.util.UUID

/**
 * All methods perform blocking SQLite work; call them from a background
 * dispatcher.
 */
class ChatRepository(context: Context) {

    private val helper = ChatDb(context.applicationContext)

    // -------------------------------------------------------- conversations

    private val conversationColumns =
        "id,title,session_id,model_id,system_prompt,created_at,updated_at,last_seen_at"

    private fun readConversation(c: android.database.Cursor): Conversation = Conversation(
        id = c.getLong(0),
        title = c.getString(1),
        sessionId = c.getString(2),
        modelId = c.getString(3),
        systemPrompt = if (c.isNull(4)) null else c.getString(4),
        createdAt = c.getLong(5),
        updatedAt = c.getLong(6),
        lastSeenAt = c.getLong(7)
    )

    fun conversations(): List<Conversation> {
        val out = ArrayList<Conversation>()
        helper.readableDatabase.rawQuery(
            "SELECT $conversationColumns FROM ${ChatDb.T_CONV} ORDER BY updated_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) out.add(readConversation(c))
        }
        return out
    }

    fun conversation(id: Long): Conversation? {
        helper.readableDatabase.rawQuery(
            "SELECT $conversationColumns FROM ${ChatDb.T_CONV} WHERE id=?",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            return readConversation(c)
        }
    }

    /** Conversations with a preview of the newest message, newest first. */
    fun conversationRows(): List<ConversationRow> {
        val out = ArrayList<ConversationRow>()
        helper.readableDatabase.rawQuery(
            "SELECT c.id,c.title,c.session_id,c.model_id,c.system_prompt,c.created_at,c.updated_at," +
                "c.last_seen_at," +
                "(SELECT content FROM ${ChatDb.T_MSG} m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1)," +
                "(SELECT role FROM ${ChatDb.T_MSG} m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1)," +
                "(SELECT COUNT(*) FROM ${ChatDb.T_MSG} m WHERE m.conversation_id=c.id " +
                "AND m.role!='user' AND m.created_at > c.last_seen_at) " +
                "FROM ${ChatDb.T_CONV} c ORDER BY c.updated_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val conversation = Conversation(
                    id = c.getLong(0),
                    title = c.getString(1),
                    sessionId = c.getString(2),
                    modelId = c.getString(3),
                    systemPrompt = if (c.isNull(4)) null else c.getString(4),
                    createdAt = c.getLong(5),
                    updatedAt = c.getLong(6),
                    lastSeenAt = c.getLong(7)
                )
                out.add(
                    ConversationRow(
                        conversation = conversation,
                        lastMessage = if (c.isNull(8)) null else c.getString(8),
                        lastRole = if (c.isNull(9)) null else c.getString(9),
                        unreadCount = c.getInt(10)
                    )
                )
            }
        }
        return out
    }

    fun createConversation(modelId: String, systemPrompt: String?): Conversation {
        val now = System.currentTimeMillis()
        val session = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("title", "New chat")
            put("session_id", session)
            put("model_id", modelId)
            put("system_prompt", systemPrompt)
            put("created_at", now)
            put("updated_at", now)
            put("last_seen_at", now)
        }
        val id = helper.writableDatabase.insert(ChatDb.T_CONV, null, values)
        return Conversation(id, "New chat", session, modelId, systemPrompt, now, now, now)
    }

    fun updateConversation(conv: Conversation) {
        val values = ContentValues().apply {
            put("title", conv.title)
            put("model_id", conv.modelId)
            put("system_prompt", conv.systemPrompt)
            put("updated_at", conv.updatedAt)
        }
        helper.writableDatabase.update(
            ChatDb.T_CONV, values, "id=?", arrayOf(conv.id.toString())
        )
    }

    fun touch(id: Long, at: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply { put("updated_at", at) }
        helper.writableDatabase.update(ChatDb.T_CONV, values, "id=?", arrayOf(id.toString()))
    }

    fun markSeen(id: Long, at: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply { put("last_seen_at", at) }
        helper.writableDatabase.update(ChatDb.T_CONV, values, "id=?", arrayOf(id.toString()))
    }

    fun deleteConversation(id: Long) {
        helper.writableDatabase.delete(ChatDb.T_CONV, "id=?", arrayOf(id.toString()))
    }

    /** Removes every conversation; messages and reminders cascade. */
    fun deleteAllConversations() {
        helper.writableDatabase.delete(ChatDb.T_CONV, null, null)
    }

    // ------------------------------------------------------------- messages

    fun messages(conversationId: Long): List<Message> {
        val out = ArrayList<Message>()
        helper.readableDatabase.rawQuery(
            "SELECT id,conversation_id,role,content,reasoning,model_id,created_at," +
                "prompt_tokens,completion_tokens,attachment_path,attachment_mime " +
                "FROM ${ChatDb.T_MSG} WHERE conversation_id=? ORDER BY id ASC",
            arrayOf(conversationId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Message(
                        id = c.getLong(0),
                        conversationId = c.getLong(1),
                        role = c.getString(2),
                        content = c.getString(3),
                        reasoning = if (c.isNull(4)) null else c.getString(4),
                        modelId = if (c.isNull(5)) null else c.getString(5),
                        createdAt = c.getLong(6),
                        promptTokens = if (c.isNull(7)) null else c.getInt(7),
                        completionTokens = if (c.isNull(8)) null else c.getInt(8),
                        attachmentPath = if (c.isNull(9)) null else c.getString(9),
                        attachmentMime = if (c.isNull(10)) null else c.getString(10)
                    )
                )
            }
        }
        return out
    }

    fun messageCount(conversationId: Long): Int {
        helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${ChatDb.T_MSG} WHERE conversation_id=?",
            arrayOf(conversationId.toString())
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    fun addMessage(message: Message): Long {
        val values = ContentValues().apply {
            put("conversation_id", message.conversationId)
            put("role", message.role)
            put("content", message.content)
            put("reasoning", message.reasoning)
            put("model_id", message.modelId)
            put("created_at", message.createdAt)
            put("prompt_tokens", message.promptTokens)
            put("completion_tokens", message.completionTokens)
            put("attachment_path", message.attachmentPath)
            put("attachment_mime", message.attachmentMime)
        }
        return helper.writableDatabase.insert(ChatDb.T_MSG, null, values)
    }

    /** Rewrites one message in place (used when editing a reply). */
    fun updateMessageContent(id: Long, content: String) {
        val values = ContentValues().apply { put("content", content) }
        helper.writableDatabase.update(ChatDb.T_MSG, values, "id=?", arrayOf(id.toString()))
    }

    /** Removes [messageId] and everything after it (used when editing a message). */
    fun deleteMessagesFrom(conversationId: Long, messageId: Long) {
        helper.writableDatabase.delete(
            ChatDb.T_MSG,
            "conversation_id=? AND id>=?",
            arrayOf(conversationId.toString(), messageId.toString())
        )
    }

    fun clearMessages(conversationId: Long) {
        helper.writableDatabase.delete(
            ChatDb.T_MSG, "conversation_id=?", arrayOf(conversationId.toString())
        )
    }

    // ------------------------------------------------------------ reminders

    private fun readReminder(c: android.database.Cursor) = Reminder(
        id = c.getLong(0),
        conversationId = c.getLong(1),
        fireAt = c.getLong(2),
        note = c.getString(3),
        status = c.getString(4),
        createdAt = c.getLong(5)
    )

    fun addReminder(conversationId: Long, fireAt: Long, note: String): Long {
        val values = ContentValues().apply {
            put("conversation_id", conversationId)
            put("fire_at", fireAt)
            put("note", note)
            put("status", "pending")
            put("created_at", System.currentTimeMillis())
        }
        return helper.writableDatabase.insert(ChatDb.T_REM, null, values)
    }

    fun reminder(id: Long): Reminder? {
        helper.readableDatabase.rawQuery(
            "SELECT id,conversation_id,fire_at,note,status,created_at FROM ${ChatDb.T_REM} WHERE id=?",
            arrayOf(id.toString())
        ).use { c -> return if (c.moveToFirst()) readReminder(c) else null }
    }

    fun pendingReminders(): List<Reminder> {
        val out = ArrayList<Reminder>()
        helper.readableDatabase.rawQuery(
            "SELECT id,conversation_id,fire_at,note,status,created_at FROM ${ChatDb.T_REM} " +
                "WHERE status='pending' ORDER BY fire_at ASC",
            null
        ).use { c -> while (c.moveToNext()) out.add(readReminder(c)) }
        return out
    }

    fun markReminderDone(id: Long) {
        val values = ContentValues().apply { put("status", "done") }
        helper.writableDatabase.update(ChatDb.T_REM, values, "id=?", arrayOf(id.toString()))
    }

    fun deleteRemindersForConversation(conversationId: Long) {
        helper.writableDatabase.delete(
            ChatDb.T_REM, "conversation_id=?", arrayOf(conversationId.toString())
        )
    }

    fun close() = helper.close()
}
