package ai.opencode.mangosteen.proactive

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

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

    /** Returns the reply with blocks removed plus every schedule it contained. */
    fun extract(content: String): Pair<String, List<Schedule>> {
        val schedules = ArrayList<Schedule>()
        val cleaned = BLOCK.replace(content) { match ->
            runCatching {
                val json = JSONObject(match.groupValues[1])
                val at = json.optString("at").ifBlank { json.optString("after") }
                val note = json.optString("note").ifBlank { "check in on the user" }
                parseTime(at)?.let { schedules.add(Schedule(it, note)) }
            }
            ""
        }.trim()
        return cleaned to schedules
    }

    private fun parseTime(value: String): Long? {
        if (value.isBlank()) return null
        for (pattern in PATTERNS) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(value)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try the next pattern
            }
        }
        // Models occasionally copy the user's relative phrasing into the block.
        return CheckinText.parse(value) ?: CheckinText.parse("in $value")
    }
}
