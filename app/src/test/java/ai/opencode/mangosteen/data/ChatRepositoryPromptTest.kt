package ai.opencode.mangosteen.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatRepositoryPromptTest {

    private lateinit var context: Context
    private lateinit var repo: ChatRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(ChatDb.NAME)
        repo = ChatRepository(context)
    }

    @After
    fun tearDown() {
        repo.close()
        context.deleteDatabase(ChatDb.NAME)
    }

    @Test
    fun `per chat system prompt survives a reload`() {
        val created = repo.createConversation("test-model", "Be kind")
        repo.updateConversation(created.copy(systemPrompt = "Be brief"))

        assertEquals("Be brief", repo.conversation(created.id)?.systemPrompt)
    }
}
