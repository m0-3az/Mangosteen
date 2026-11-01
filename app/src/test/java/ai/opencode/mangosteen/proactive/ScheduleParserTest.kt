package ai.opencode.mangosteen.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScheduleParserTest {

    private val now = at(2026, Calendar.OCTOBER, 15, 12, 0)

    @Test
    fun `a schedule in the past is dropped`() {
        val (text, schedules) = ScheduleParser.extract(
            "Sure.\n```schedule\n{\"at\":\"2026-10-14T09:00\",\"note\":\"old\"}\n```",
            now,
        )

        assertEquals("Sure.", text)
        assertTrue(schedules.isEmpty())
    }

    @Test
    fun `an impossible future calendar date is rejected`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"2027-02-30T09:00\",\"note\":\"bad\"}\n```",
            now,
        )

        assertTrue(schedules.isEmpty())
    }

    @Test
    fun `trailing garbage is rejected`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"2026-10-20T09:00:00junk\",\"note\":\"bad\"}\n```",
            now,
        )

        assertTrue(schedules.isEmpty())
    }

    @Test
    fun `a zoned time is honoured`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"2026-10-20T09:00:00Z\",\"note\":\"utc\"}\n```",
            now,
        )

        assertEquals(atUtc(2026, Calendar.OCTOBER, 20, 9, 0), schedules.single().fireAt)
    }

    @Test
    fun `a future local time is kept`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"2026-10-20 09:30\",\"note\":\"local\"}\n```",
            now,
        )

        assertEquals(at(2026, Calendar.OCTOBER, 20, 9, 30), schedules.single().fireAt)
    }

    @Test
    fun `relative fallback still schedules the future`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"in 2 hours\",\"note\":\"relative\"}\n```",
            now,
        )

        assertEquals(now + 2 * 3_600_000L, schedules.single().fireAt)
    }

    @Test
    fun `an offset zone is honoured`() {
        val (_, schedules) = ScheduleParser.extract(
            "```schedule\n{\"at\":\"2026-10-20T09:00:00+02:00\",\"note\":\"offset\"}\n```",
            now,
        )

        assertEquals(atUtc(2026, Calendar.OCTOBER, 20, 7, 0), schedules.single().fireAt)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(year, month, day, hour, minute).timeInMillis

    private fun atUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
