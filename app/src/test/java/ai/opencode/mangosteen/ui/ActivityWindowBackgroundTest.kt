package ai.opencode.mangosteen.ui

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import ai.opencode.mangosteen.ChatActivity
import ai.opencode.mangosteen.R
import ai.opencode.mangosteen.data.ChatRepository
import ai.opencode.mangosteen.settings.SecurePrefs
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The system starting window is drawn from the manifest theme before any
 * activity code runs, so the actual flash cannot be asserted here. These cover
 * the two pieces that can: the splash colour the system reads, and the selected
 * theme being on the window by the time onCreate returns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActivityWindowBackgroundTest {

    @Test
    fun `manifest theme uses the branded splash background instead of black`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val controller = createChatActivity(app)
        val attributes = controller.get()
            .obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val background = attributes.getDrawable(0)
        attributes.recycle()

        val color = (background as ColorDrawable).color
        assertEquals(app.getColor(R.color.primary), color)
        assertNotEquals(Color.BLACK, color)
        controller.destroy()
    }

    @Test
    fun `selected light theme is applied to the activity window`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SecurePrefs(app).setThemeId("porcelain")

        val controller = createChatActivity(app)
        assertEquals(
            0xFFF7F6EE.toInt(),
            (controller.get().window.decorView.background as ColorDrawable).color,
        )
        controller.destroy()
    }

    @Test
    fun `selected dark theme is applied to the activity window`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SecurePrefs(app).setThemeId("amoled")

        val controller = createChatActivity(app)
        assertEquals(
            Color.BLACK,
            (controller.get().window.decorView.background as ColorDrawable).color,
        )
        controller.destroy()
    }

    private fun createChatActivity(app: Application): ActivityController<ChatActivity> {
        val repo = ChatRepository(app)
        val conversation = repo.createConversation("m", null)
        repo.close()
        return Robolectric.buildActivity(
            ChatActivity::class.java,
            Intent(app, ChatActivity::class.java)
                .putExtra(ChatActivity.EXTRA_ID, conversation.id),
        ).create()
    }
}
