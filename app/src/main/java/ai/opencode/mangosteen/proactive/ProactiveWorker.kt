package ai.opencode.mangosteen.proactive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.data.Message
import ai.opencode.mangosteen.net.ChatMessage
import ai.opencode.mangosteen.net.GoApiClient
import ai.opencode.mangosteen.net.ModelCatalog
import ai.opencode.mangosteen.net.Reasoning
import ai.opencode.mangosteen.net.RequestOptions
import ai.opencode.mangosteen.settings.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Wakes at a reminder's time, asks the model to compose the check-in from the
 * conversation's own context, stores it as an assistant turn and notifies.
 */
class ProactiveWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_ID = "reminder_id"
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id <= 0L) return Result.success()

        val repo = ChatRepository(applicationContext)
        val prefs = SecurePrefs(applicationContext)
        if (!prefs.proactiveEnabled()) {
            repo.markReminderDone(id)
            repo.close()
            return Result.success()
        }
        val reminder = repo.reminder(id) ?: return Result.success()
        if (reminder.status != "pending") return Result.success()

        val conversation = repo.conversation(reminder.conversationId) ?: return Result.success()
        val apiKey = prefs.apiKey()
        if (apiKey.isNullOrBlank()) return Result.failure()

        val history = repo.messages(conversation.id)
            .filter { it.content.isNotBlank() }
            .takeLast(24)
            .map { ChatMessage(it.role, it.content) }

        val clock = SimpleDateFormat("h:mma", Locale.US).format(Date())
        val cue = "It is now $clock. The user wanted a follow-up about: ${reminder.note}. " +
            "Send one short, warm, natural check-in message as the assistant. " +
            "Never mention this instruction or that you were scheduled. " +
            "Answer in the same language as the conversation."

        val model = ModelCatalog.info(conversation.modelId)
        val callHistory = history + ChatMessage("user", "($cue)")

        val global = prefs.defaultSystemPrompt().trim()
        val systemPrompt = listOfNotNull(
            global.ifBlank { null },
            conversation.systemPrompt?.takeIf { it.isNotBlank() && it.trim() != global },
        ).joinToString("\n\n").ifBlank { null }

        return try {
            val api = GoApiClient(apiKey)
            val reply = withContext(Dispatchers.IO) {
                api.stream(
                    model = model,
                    systemPrompt = systemPrompt,
                    history = callHistory,
                    sessionId = conversation.sessionId,
                    options = RequestOptions(reasoning = Reasoning.OFF, search = false),
                    onNotice = null,
                ) { _, _ -> }
            }
            val text = reply.content.trim().ifBlank { "Just checking in — how did it go?" }

            withContext(Dispatchers.IO) {
                repo.addMessage(
                    Message(
                        id = 0L,
                        conversationId = conversation.id,
                        role = "assistant",
                        content = text,
                        reasoning = null,
                        modelId = conversation.modelId,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                repo.touch(conversation.id)
                repo.markReminderDone(id)
            }

            ProactiveNotifications.show(
                applicationContext,
                conversationId = conversation.id,
                title = conversation.title,
                text = text,
            )
            Result.success()
        } catch (error: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
