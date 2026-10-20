package ai.opencode.mangosteen.proactive

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ai.opencode.mangosteen.data.Reminder
import java.util.concurrent.TimeUnit

/** Registers one persisted background job per pending reminder. */
object ProactiveScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val delay = (reminder.fireAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ProactiveWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ProactiveWorker.KEY_ID to reminder.id))
            .addTag(tag(reminder.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            tag(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(tag(reminderId))
    }

    private fun tag(id: Long) = "mangosteen-reminder-$id"
}
