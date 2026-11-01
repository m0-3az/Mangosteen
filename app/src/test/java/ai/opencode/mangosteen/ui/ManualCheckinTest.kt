package ai.opencode.mangosteen.ui

import android.app.Application
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.data.Reminder
import ai.opencode.mangosteen.settings.SecurePrefs
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualCheckinTest {

    private lateinit var app: Application
    private lateinit var repo: ChatRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(dispatcher)
        repo = ChatRepository(app)
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, config)
    }

    @After
    fun tearDown() {
        repo.close()
        WorkManagerTestInitHelper.closeWorkDatabase()
        Dispatchers.resetMain()
    }

    @Test
    fun `manual check-in warns instead of silently dropping when proactive is off`() = runTest(dispatcher) {
        SecurePrefs(app).setProactiveEnabled(false)
        val conversation = repo.createConversation("m", null)
        val scheduled = mutableListOf<Reminder>()
        val model = ChatViewModel(app, dispatcher) { _, reminder -> scheduled += reminder }
        model.conversation = conversation

        model.scheduleCheckIn(System.currentTimeMillis() + 3_600_000L)
        advanceUntilIdle()

        assertEquals("Turn on proactive check-ins in Settings first", model.toast)
        assertTrue(repo.pendingReminders().none { it.conversationId == conversation.id })
        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun `manual check-in persists a pending reminder and schedules it when proactive is on`() =
        runTest(dispatcher) {
            SecurePrefs(app).setProactiveEnabled(true)
            val conversation = repo.createConversation("m", null)
            val scheduled = mutableListOf<Reminder>()
            val model = ChatViewModel(app, dispatcher) { _, reminder -> scheduled += reminder }
            model.conversation = conversation
            val fireAt = System.currentTimeMillis() + 3_600_000L

            model.scheduleCheckIn(fireAt)
            advanceUntilIdle()

            val pending = repo.pendingReminders().filter { it.conversationId == conversation.id }
            assertEquals(1, pending.size)
            assertEquals(fireAt, pending.single().fireAt)
            assertEquals("check in on the user", pending.single().note)

            assertEquals(1, scheduled.size)
            assertEquals(pending.single().id, scheduled.single().id)
            assertEquals(conversation.id, scheduled.single().conversationId)
            assertEquals(fireAt, scheduled.single().fireAt)
            assertTrue(model.toast.orEmpty().startsWith("Check-in scheduled for "))
        }

    @Test
    fun `manual check-in enqueues WorkManager work through the production scheduler`() =
        runTest(dispatcher) {
            SecurePrefs(app).setProactiveEnabled(true)
            val conversation = repo.createConversation("m", null)
            val model = ChatViewModel(app, dispatcher)
            model.conversation = conversation
            val fireAt = System.currentTimeMillis() + 3_600_000L

            model.scheduleCheckIn(fireAt)
            advanceUntilIdle()

            val reminder = repo.pendingReminders()
                .single { it.conversationId == conversation.id }
            val workInfos = WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("mangosteen-reminder-${reminder.id}")
                .get(5, TimeUnit.SECONDS)

            assertEquals(1, workInfos.size)
            assertEquals(WorkInfo.State.ENQUEUED, workInfos.single().state)
        }
}
