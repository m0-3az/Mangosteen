package ai.opencode.mangosteen.ui

import android.app.Application
import ai.opencode.mangosteen.data.Conversation
import ai.opencode.mangosteen.settings.SecurePrefs
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualCheckinTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `manual check-in warns instead of silently dropping when proactive is off`() {
        SecurePrefs(app).setProactiveEnabled(false)
        val model = ChatViewModel(app)
        model.conversation = Conversation(
            id = 1L,
            title = "test",
            sessionId = "s",
            modelId = "m",
            systemPrompt = null,
            createdAt = 0L,
            updatedAt = 0L,
        )

        model.scheduleCheckIn(System.currentTimeMillis() + 3_600_000L)

        assertEquals("Turn on proactive check-ins in Settings first", model.toast)
    }

    @Test
    fun `manual check-in is allowed when proactive is on`() {
        SecurePrefs(app).setProactiveEnabled(true)
        val model = ChatViewModel(app)
        model.conversation = Conversation(
            id = 2L,
            title = "test",
            sessionId = "s",
            modelId = "m",
            systemPrompt = null,
            createdAt = 0L,
            updatedAt = 0L,
        )

        model.scheduleCheckIn(System.currentTimeMillis() + 3_600_000L)

        assertNull(model.toast)
    }
}
