package ai.opencode.mangosteen.proactive

import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Pulls `schedule` blocks out of a model reply, leaving clean text behind. */
object ScheduleParser {

    data class Schedule(val fireAt: Long, val note: String)

    private val BLOCK = Regex(
        "```schedule\\s*(\\{.*?\\})\\s*```",
        RegexOption.DOT_MATCHES_ALL
    )

    private val PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
    )

    private val ZONE_SUFFIX = Regex("(Z|[+-]\\d{2}:?\\d{2})$", RegexOption.IGNORE_CASE)

    /** Returns the reply with blocks removed plus every future schedule it contained. */
    fun extract(
        content: String,
        now: Long = System.currentTimeMillis(),
    ): Pair<String, List<Schedule>> {
        val schedules = ArrayList<Schedule>()
        val cleaned = BLOCK.replace(content) { match ->
            runCatching {
                val json = JSONObject(match.groupValues[1])
                val at = json.optString("at").ifBlank { json.optString("after") }
                val note = json.optString("note").ifBlank { "check in on the user" }
                parseTime(at, now)?.takeIf { it > now }?.let {
                    schedules.add(Schedule(it, note))
                }
            }
            ""
        }.trim()
        return cleaned to schedules
    }

    private fun parseTime(value: String, now: Long): Long? {
        if (value.isBlank()) return null
        val zoneMatch = ZONE_SUFFIX.find(value)
        val core = if (zoneMatch != null) {
            value.substring(0, zoneMatch.range.first).trim()
        } else {
            value.trim()
        }
        val zone = zoneMatch?.let { timeZone(it.value) }
        if (zoneMatch != null && zone == null) return null
        parseLocal(core, zone)?.let { return it }
        if (zoneMatch != null) return null
        // Models occasionally copy the user's relative phrasing into the block.
        return CheckinText.parse(value, now) ?: CheckinText.parse("in $value", now)
    }

    private fun parseLocal(value: String, zone: TimeZone?): Long? {
        for (pattern in PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (zone != null) timeZone = zone
            }
            val position = ParsePosition(0)
            try {
                val parsed = format.parse(value, position) ?: continue
                if (position.index == value.length) return parsed.time
            } catch (_: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    private fun timeZone(suffix: String): TimeZone? {
        if (suffix.equals("Z", ignoreCase = true)) return TimeZone.getTimeZone("UTC")
        val normalized = if (suffix.length == 5) {
            suffix.substring(0, 3) + ":" + suffix.substring(3)
        } else {
            suffix
        }
        return TimeZone.getTimeZone("GMT$normalized")
    }
}
