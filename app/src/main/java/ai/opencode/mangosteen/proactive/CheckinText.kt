package ai.opencode.mangosteen.proactive

import java.util.Calendar

/**
 * Best-effort fire time for `/checkin <text>`. Understands relative offsets
 * ("in 2 hours", "after ten minutes", "in half an hour", "2 days from now"),
 * absolute days and times ("tomorrow at 9am", "tonight", "on friday at 5pm"),
 * and carries a day or am/pm forward to bare times that follow it ("exam
 * tomorrow at 9am, finish at 10" -> tomorrow 10am). The last time mentioned
 * wins, so a check-in lands just after the event. Returns null when the text
 * holds no usable time.
 */
object CheckinText {

    private val RELATIVE = Regex(
        "\\b(?:in|after)\\s+" +
            "(?:(\\d+(?:\\.\\d+)?)\\s*" +
            "|(?:(an?|one|two|three|four|five|six|seven|eight|nine|ten|eleven|" +
            "twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|" +
            "nineteen|twenty|thirty|forty|fifty|sixty|ninety)\\s+))" +
            "(hours?|hrs?|h|minutes?|mins?|m|days?|d|weeks?|w)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val HALF = Regex(
        "\\b(?:in|after)\\s+(?:a\\s+)?half\\s+(?:an?\\s+)?(hour|day|week)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val FROM_NOW = Regex(
        "\\b(\\d+(?:\\.\\d+)?|an?|one|two|three|four|five|six|seven|eight|nine|ten|" +
            "eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|" +
            "nineteen|twenty|thirty|forty|fifty|sixty|ninety)\\s+" +
            "(hours?|hrs?|h|minutes?|mins?|m|days?|d|weeks?|w)\\s+from\\s+now\\b",
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

    private val NUMBER_WORDS = mapOf(
        "a" to 1.0,
        "an" to 1.0,
        "one" to 1.0,
        "two" to 2.0,
        "three" to 3.0,
        "four" to 4.0,
        "five" to 5.0,
        "six" to 6.0,
        "seven" to 7.0,
        "eight" to 8.0,
        "nine" to 9.0,
        "ten" to 10.0,
        "eleven" to 11.0,
        "twelve" to 12.0,
        "thirteen" to 13.0,
        "fourteen" to 14.0,
        "fifteen" to 15.0,
        "sixteen" to 16.0,
        "seventeen" to 17.0,
        "eighteen" to 18.0,
        "nineteen" to 19.0,
        "twenty" to 20.0,
        "thirty" to 30.0,
        "forty" to 40.0,
        "fifty" to 50.0,
        "sixty" to 60.0,
        "ninety" to 90.0,
    )

    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val hits = ArrayList<Pair<Int, Long>>() // match position to fire time

        for (match in RELATIVE.findAll(text)) {
            val amount = match.groupValues[1].toDoubleOrNull()
                ?: NUMBER_WORDS[match.groupValues[2].lowercase()]
                ?: continue
            if (amount <= 0.0) continue
            val unit = match.groupValues[3]
            hits += match.range.first to (now + duration(amount, unit))
        }

        for (match in HALF.findAll(text)) {
            val unit = match.groupValues[1]
            hits += match.range.first to (now + duration(0.5, unit))
        }

        for (match in FROM_NOW.findAll(text)) {
            val amount = match.groupValues[1].toDoubleOrNull()
                ?: NUMBER_WORDS[match.groupValues[1].lowercase()]
                ?: continue
            if (amount <= 0.0) continue
            val unit = match.groupValues[2]
            hits += match.range.first to (now + duration(amount, unit))
        }

        var dayStart: Long? = null
        var meridiem: Boolean? = null // true = pm, carried to later bare times

        val dayMatches = DAY.findAll(text).map { Triple(0, it.range.first, it) }
        val atMatches = AT.findAll(text).map { Triple(1, it.range.first, it) }
        for ((kind, _, match) in (dayMatches + atMatches).sortedBy { it.second }) {
            val groups = match.groupValues
            if (kind == 0) {
                val word = groups[1].lowercase()
                val base = dayFor(word, now)
                dayStart = base
                val evening = word.startsWith("ton")
                val preferred = base + (if (evening) 20 else 9) * 3_600_000L
                hits += match.range.first to (if (preferred > now) preferred else preferred + DAY_MS)
            } else {
                val hour = groups[1].toIntOrNull() ?: continue
                if (hour > 23) continue
                val minute = groups[2].toIntOrNull()?.coerceAtMost(59) ?: 0
                val mer = groups[3].lowercase()
                val pm = when {
                    mer.startsWith("p") -> true
                    mer.startsWith("a") -> false
                    meridiem != null -> meridiem
                    else -> hour !in 8..11 // 1-7 reads as afternoon, 8-11 as morning
                }
                if (mer.isNotEmpty()) meridiem = pm
                hits += match.range.first to resolve(
                    dayStart,
                    now,
                    (hour % 12) + if (pm) 12 else 0,
                    minute,
                )
            }
        }

        return hits.maxByOrNull { it.first }?.second
    }

    /** Converts a relative amount and unit into milliseconds. */
    fun duration(amount: Double, unit: String): Long {
        val perUnit = when (unit.firstOrNull()?.lowercaseChar()) {
            'h' -> 3_600_000L
            'd' -> 86_400_000L
            'w' -> 604_800_000L
            else -> 60_000L
        }
        return (amount * perUnit).toLong()
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
                    while (cal.get(Calendar.DAY_OF_WEEK) - 1 != target) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
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
        val time = cal.timeInMillis
        return if (time > now) time else time + DAY_MS
    }

    private const val DAY_MS = 24 * 3_600_000L
}
