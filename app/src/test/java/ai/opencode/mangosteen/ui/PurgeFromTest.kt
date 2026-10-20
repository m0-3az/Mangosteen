package ai.opencode.mangosteen.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PurgeFromTest {

    @Test
    fun `keeps messages before the index and drops it plus everything after`() {
        val list = mutableListOf(
            message("user", "one"),
            message("assistant", "two"),
            message("user", "three"),
            message("assistant", "four"),
        )

        val kept = list.purgeFrom(2)

        assertEquals(listOf("one", "two"), kept.map { it.content })
        assertEquals(2, list.size)
    }

    @Test
    fun `purging from the first message empties the thread`() {
        val list = mutableListOf(message("user", "one"), message("assistant", "two"))

        list.purgeFrom(0)

        assertEquals(0, list.size)
    }

    @Test
    fun `purging past the last message keeps everything`() {
        val list = mutableListOf(message("user", "one"))

        val kept = list.purgeFrom(1)

        assertEquals(listOf("one"), kept.map { it.content })
        assertEquals(1, list.size)
    }

    private fun message(role: String, content: String) =
        UiMessage(0L, role, content, null)
}
