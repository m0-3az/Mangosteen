package ai.opencode.mangosteen.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

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
    fun `understands after and spelled out numbers`() {
        assertEquals(now + 2 * 3_600_000L, CheckinText.parse("check in on me after two hours", now))
        assertEquals(
            now + 10 * 60_000L,
            CheckinText.parse("remind me after 10 minutes to submit my task", now),
        )
        assertEquals(now + 3 * 3_600_000L, CheckinText.parse("nudge me in three hours", now))
    }

    @Test
    fun `understands days weeks and fractional or half units`() {
        assertEquals(now + 2 * 86_400_000L, CheckinText.parse("follow up in 2 days", now))
        assertEquals(now + 2 * 86_400_000L, CheckinText.parse("2 days from now", now))
        assertEquals(now + 7 * 86_400_000L, CheckinText.parse("check in after a week", now))
        assertEquals(now + 90 * 60_000L, CheckinText.parse("after 1.5 hours", now))
        assertEquals(now + 30 * 60_000L, CheckinText.parse("in half an hour", now))
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

    @Test
    fun `relative days combine with a clock time`() {
        assertEquals(
            at(2026, Calendar.OCTOBER, 17, 9, 0),
            CheckinText.parse("check in in 2 days at 9am", now),
        )
    }

    @Test
    fun `chained relative units add up`() {
        assertEquals(
            now + 150 * 60_000L,
            CheckinText.parse("remind me in 2 hours and 30 minutes", now),
        )
        assertEquals(
            now + 150 * 60_000L,
            CheckinText.parse("remind me in 2 hours, 30 minutes", now),
        )
    }

    @Test
    fun `a bare time after am does not inherit am`() {
        assertEquals(
            at(2026, Calendar.OCTOBER, 16, 13, 0),
            CheckinText.parse("tomorrow at 9am, finish at 1", now),
        )
    }

    @Test
    fun `tonight keeps an evening clock`() {
        assertEquals(
            at(2026, Calendar.OCTOBER, 15, 21, 0),
            CheckinText.parse("tonight at 9", now),
        )
    }

    @Test
    fun `relative days keep wall clock time across dst`() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        try {
            val start = at(2026, Calendar.OCTOBER, 24, 9, 0)
            assertEquals(
                at(2026, Calendar.OCTOBER, 26, 9, 0),
                CheckinText.parse("in 2 days at 9am", start),
            )
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `absurd offsets do not overflow into an instant check-in`() {
        assertNull(CheckinText.parse("in 200000000000 days", now))
        assertNull(CheckinText.parse("in 999999999999999999999 hours", now))
    }

    @Test
    fun `zero negative and past offsets do not produce invalid check-ins`() {
        assertNull(CheckinText.parse("in 0 minutes", now))
        assertNull(CheckinText.parse("after 0 hours", now))
        assertNull(CheckinText.parse("in -10 minutes", now))
        assertNull(CheckinText.parse("after -2 days", now))

        val afternoon = at(2026, Calendar.OCTOBER, 15, 15, 0)
        val fireAt = CheckinText.parse("at 9am", afternoon)
        assertEquals(at(2026, Calendar.OCTOBER, 16, 9, 0), fireAt)
        assertTrue(fireAt != null && fireAt > afternoon)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(year, month, day, hour, minute).timeInMillis
}
