package ai.opencode.mangosteen.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SecurePrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the global system prompt survives a new prefs instance`() {
        SecurePrefs(context).setDefaultSystemPrompt("Answer in haiku")

        assertEquals("Answer in haiku", SecurePrefs(context).defaultSystemPrompt())
    }

    @Test
    fun `clearing the global system prompt persists`() {
        SecurePrefs(context).setDefaultSystemPrompt("temporary")
        SecurePrefs(context).setDefaultSystemPrompt("")

        assertEquals("", SecurePrefs(context).defaultSystemPrompt())
    }
}
