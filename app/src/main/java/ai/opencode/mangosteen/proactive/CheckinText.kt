package ai.opencode.mangosteen.proactive

import java.util.Calendar

/**
 * Best-effort fire time for `/checkin <text>`. Understands "in 2 hours",
 * "tomorrow at 9am", "tonight", "on friday at 5pm", and carries a day or
 * am/pm forward to bare times that follow it ("exam tomorrow at 9am, finish
 * at 10" → tomorrow 10am). The last time mentioned wins, so a check-in lands
 * just after the event. Returns null when the text holds no usable time.
 */
object CheckinText {

    private val IN = Regex(
        "\\bin\\s+(\\d+)\\s*(hours?|hrs?|h|minutes?|mins?|m)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val AT = Regex(
        "\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?",
        RegexOption.IGNORE_CASE,
    )
    private val DAY = Regex(
        "\\b(today|tonight|tomorrow|mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|" +
            "thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val WEEKDAYS =
        listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val hits = ArrayList<Pair<Int, Long>>() // match position to fire time

        for (m in IN.findAll(text)) {
            val n = m.groupValues[1].toIntOrNull() ?: continue
            val ms = if (m.groupValues[2].startsWith("h", ignoreCase = true)) {
                n * 3_600_000L
            } else {
                n * 60_000L
            }
            hits += m.range.first to (now + ms)
        }

        var dayStart: Long? = null
        var meridiem: Boolean? = null // true = pm, carried to later bare times

        val dayMatches = DAY.findAll(text).map { Triple(0, it.range.first, it) }
        val atMatches = AT.findAll(text).map { Triple(1, it.range.first, it) }
        for ((kind, _, match) in (dayMatches + atMatches).sortedBy { it.second }) {
            val g = match.groupValues
            if (kind == 0) {
                val word = g[1].lowercase()
                val base = dayFor(word, now)
                dayStart = base
                val evening = word.startsWith("ton")
                val preferred = base + (if (evening) 20 else 9) * 3_600_000L
                hits += match.range.first to (if (preferred > now) preferred else preferred + DAY_MS)
            } else {
                val hour = g[1].toIntOrNull() ?: continue
                if (hour > 23) continue
                val minute = g[2].toIntOrNull()?.coerceAtMost(59) ?: 0
                val mer = g[3].lowercase()
                val pm = when {
                    mer.startsWith("p") -> true
                    mer.startsWith("a") -> false
                    meridiem != null -> meridiem
                    else -> hour !in 8..11 // 1-7 reads as afternoon, 8-11 as morning
                }
                if (mer.isNotEmpty()) meridiem = pm
                hits += match.range.first to resolve(dayStart, now, (hour % 12) + if (pm) 12 else 0, minute)
            }
        }

        return hits.maxByOrNull { it.first }?.second
    }

    /** Midnight of the day a day word points to. Unknown words keep today. */
    private fun dayFor(word: String, now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when {
            word.startsWith("tom") -> cal.add(Calendar.DAY_OF_YEAR, 1)
            else -> {
                val target = WEEKDAYS.indexOfFirst { it.startsWith(word.take(3)) }
                if (target >= 0) {
                    while (cal.get(Calendar.DAY_OF_WEEK) - 1 != target) cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return cal.timeInMillis
    }

    private fun resolve(dayStart: Long?, now: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayStart ?: now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t = cal.timeInMillis
        return if (t > now) t else t + DAY_MS
    }

    private const val DAY_MS = 24 * 3_600_000L
}
