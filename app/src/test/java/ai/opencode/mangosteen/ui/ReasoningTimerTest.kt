package ai.opencode.mangosteen.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReasoningTimerTest {

    private var now = 1_000L
    private val timer = ReasoningTimer { now }

    @Test
    fun `timer starts at request time and freezes on the first content chunk`() {
        timer.start()
        now = 4_000L
        timer.onReasoning()
        now = 6_000L
        timer.onContent()
        now = 9_000L

        assertEquals(5_000L, timer.finish())
    }

    @Test
    fun `a reasoning chunk at T plus 1500ms reports 1500ms at T plus 4000ms`() {
        timer.start()
        timer.onReasoning()
        now = 2_500L
        timer.onContent()
        now = 5_000L

        assertEquals(1_500L, timer.finish())
        assertEquals("Thought for 2 seconds", thinkingLabel(1_500L, live = false))
    }

    @Test
    fun `one reasoning chunk still includes first token latency`() {
        timer.start()
        now = 3_500L
        timer.onReasoning()
        timer.onContent()
        now = 10_000L

        assertEquals(2_500L, timer.finish())
    }

    @Test
    fun `a reply without reasoning has no duration`() {
        timer.start()
        now = 2_000L
        timer.onContent()

        assertNull(timer.finish())
    }

    @Test
    fun `a stream that never sends content falls back to now`() {
        timer.start()
        now = 5_000L
        timer.onReasoning()
        now = 8_000L

        assertEquals(7_000L, timer.finish())
    }
}
