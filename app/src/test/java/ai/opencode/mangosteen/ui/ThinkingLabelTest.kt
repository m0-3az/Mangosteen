package ai.opencode.mangosteen.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ThinkingLabelTest {

    @Test
    fun `live reasoning says thinking`() {
        assertEquals("Thinking…", thinkingLabel(null, live = true))
    }

    @Test
    fun `a finished reply no longer says thinking`() {
        assertEquals("Thought for 4 seconds", thinkingLabel(4_000L, live = false))
    }

    @Test
    fun `very short reasoning uses the friendly fallback`() {
        assertEquals("Thought for a moment", thinkingLabel(500L, live = false))
        assertEquals("Thought for a moment", thinkingLabel(null, live = false))
    }

    @Test
    fun `minutes and seconds are formatted`() {
        assertEquals("Thought for 1m 5s", thinkingLabel(65_000L, live = false))
        assertEquals("Thought for 2m", thinkingLabel(120_000L, live = false))
    }
}
