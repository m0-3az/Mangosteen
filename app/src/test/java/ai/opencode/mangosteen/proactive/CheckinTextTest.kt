package ai.opencode.mangosteen.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class CheckinTextTest {

    // Thursday, October 15 2026, 00:35 local.
    private val now = at(2026, Calendar.OCTOBER, 15, 0, 35)

    @Test
    fun `carries the day and am pm to a later bare time`() {
        val fireAt = CheckinText.parse("my exam is tomorrow at 9am and i finish at 10.", now)
        assertEquals(at(2026, Calendar.OCTOBER, 16, 10, 0), fireAt)
    }

    @Test
    fun `understands a relative offset`() {
        assertEquals(now + 20 * 60_000L, CheckinText.parse("remind me in 20 minutes", now))
        assertEquals(now + 2 * 3_600_000L, CheckinText.parse("check in in 2 hours please", now))
    }

    @Test
    fun `bare time later today stays today`() {
        assertEquals(
            at(2026, Calendar.OCTOBER, 15, 18, 0),
            CheckinText.parse("call me at 6pm", now),
        )
    }

    @Test
    fun `bare time already past rolls to tomorrow`() {
        val afternoon = at(2026, Calendar.OCTOBER, 15, 15, 0)
        assertEquals(
            at(2026, Calendar.OCTOBER, 16, 14, 0),
            CheckinText.parse("at 2pm", afternoon),
        )
    }

    @Test
    fun `day word alone picks a sensible hour`() {
        assertEquals(at(2026, Calendar.OCTOBER, 16, 9, 0), CheckinText.parse("check in tomorrow", now))
        assertEquals(at(2026, Calendar.OCTOBER, 15, 20, 0), CheckinText.parse("check in tonight", now))
    }

    @Test
    fun `weekday resolves to its next occurrence`() {
        assertEquals(
            at(2026, Calendar.OCTOBER, 16, 17, 0),
            CheckinText.parse("party on friday at 5pm", now),
        )
    }

    @Test
    fun `no time in the text means no check-in`() {
        assertNull(CheckinText.parse("whenever you like", now))
        assertNull(CheckinText.parse("", now))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(year, month, day, hour, minute).timeInMillis
}
