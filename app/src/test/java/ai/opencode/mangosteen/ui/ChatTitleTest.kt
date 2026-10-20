package ai.opencode.mangosteen.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTitleTest {

    @Test
    fun `plain title passes through`() {
        assertEquals("Interview prep", ChatTitle.clean("Interview prep"))
    }

    @Test
    fun `strips quotes, a title prefix and trailing punctuation`() {
        assertEquals(
            "Money left after spending",
            ChatTitle.clean("\"Title: Money left after spending.\""),
        )
    }

    @Test
    fun `unwraps a fenced reply and skips blank lines`() {
        assertEquals("Exam nerves", ChatTitle.clean("```\n\nExam nerves\n```"))
    }

    @Test
    fun `takes only the first non-blank line`() {
        assertEquals("First date", ChatTitle.clean("First date\n\nSecond line ignored"))
    }

    @Test
    fun `caps the length and rejects empty answers`() {
        assertEquals(48, ChatTitle.clean("x".repeat(80))?.length)
        assertNull(ChatTitle.clean("   \n  "))
        assertNull(ChatTitle.clean("\"\"."))
    }
}
