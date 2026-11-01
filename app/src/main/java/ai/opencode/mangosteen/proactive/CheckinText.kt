package ai.opencode.mangosteen.proactive

import java.util.Calendar

/**
 * Best-effort fire time for `/checkin <text>`. Understands relative offsets
 * ("in 2 hours", "after ten minutes", "in half an hour", "2 days from now"),
 * absolute days and times ("tomorrow at 9am", "tonight", "on friday at 5pm"),
 * and combines the two ("in 2 days at 9am", "in 2 hours and 30 minutes").
 * A later clock time applies to the date or offset established before it, so
 * "tomorrow at 9am, finish at 1" means tomorrow at 1pm. Returns null when the
 * text holds no usable time or the result is absurdly far away.
 */
object CheckinText {

    private const val WORDS =
        "an?|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|" +
            "thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|" +
            "twenty|thirty|forty|fifty|sixty|ninety"
    private const val AMOUNT = "(\\d+(?:\\.\\d+)?|(?:$WORDS))"
    private const val UNIT = "(hours?|hrs?|h|minutes?|mins?|m|days?|d|weeks?|w)"

    private val RELATIVE = Regex("\\b(?:in|after)\\s+$AMOUNT\\s*$UNIT\\b", RegexOption.IGNORE_CASE)
    private val HALF = Regex(
        "\\b(?:in|after)\\s+(?:a\\s+)?half\\s+(?:an?\\s+)?(hour|day|week)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val FROM_NOW = Regex(
        "\\b$AMOUNT\\s+$UNIT\\s+from\\s+now\\b",
        RegexOption.IGNORE_CASE,
    )
    private val CHAINED = Regex("(?:\\band\\b|,)\\s*$AMOUNT\\s*$UNIT\\b", RegexOption.IGNORE_CASE)
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

    private sealed interface Piece {
        val start: Int
        val end: Int

        data class Offset(
            override val start: Int,
            override val end: Int,
            val amount: Double,
            val unit: String,
            val chained: Boolean,
        ) : Piece

        data class Day(override val start: Int, override val end: Int, val word: String) : Piece

        data class Clock(
            override val start: Int,
            override val end: Int,
            val hour: Int,
            val minute: Int,
            val mer: String,
        ) : Piece
    }

    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val pieces = ArrayList<Piece>()
        val relativeSpans = ArrayList<IntRange>()

        for (match in RELATIVE.findAll(text)) {
            val amount = parseAmount(match.groupValues[1]) ?: continue
            if (!withinLimit(amount, match.groupValues[2])) continue
            pieces += Piece.Offset(
                match.range.first,
                match.range.last + 1,
                amount,
                match.groupValues[2],
                chained = false,
            )
            relativeSpans += match.range
        }

        for (match in HALF.findAll(text)) {
            if (relativeSpans.any { it.first <= match.range.last && match.range.first <= it.last }) {
                continue
            }
            pieces += Piece.Offset(
                match.range.first,
                match.range.last + 1,
                0.5,
                match.groupValues[1],
                chained = false,
            )
            relativeSpans += match.range
        }

        for (match in FROM_NOW.findAll(text)) {
            if (relativeSpans.any { it.first <= match.range.last && match.range.first <= it.last }) {
                continue
            }
            val amount = parseAmount(match.groupValues[1]) ?: continue
            if (!withinLimit(amount, match.groupValues[2])) continue
            pieces += Piece.Offset(
                match.range.first,
                match.range.last + 1,
                amount,
                match.groupValues[2],
                chained = false,
            )
            relativeSpans += match.range
        }

        for (match in CHAINED.findAll(text)) {
            val previous = pieces
                .filter { it.end <= match.range.first }
                .maxByOrNull { it.end } ?: continue
            if (previous !is Piece.Offset) continue
            if (!text.substring(previous.end, match.range.first).isBlank()) continue
            val amount = parseAmount(match.groupValues[1]) ?: continue
            if (!withinLimit(amount, match.groupValues[2])) continue
            pieces += Piece.Offset(
                match.range.first,
                match.range.last + 1,
                amount,
                match.groupValues[2],
                chained = true,
            )
        }

        for (match in DAY.findAll(text)) {
            pieces += Piece.Day(match.range.first, match.range.last + 1, match.groupValues[1])
        }

        for (match in AT.findAll(text)) {
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            if (hour > 23) continue
            pieces += Piece.Clock(
                match.range.first,
                match.range.last + 1,
                hour,
                match.groupValues[2].toIntOrNull()?.coerceAtMost(59) ?: 0,
                match.groupValues[3],
            )
        }

        val base = Calendar.getInstance().apply { timeInMillis = now }
        var evening = false
        var fireAt: Long? = null
        var previousClock: Long? = null

        for (piece in pieces.sortedBy { it.start }) {
            when (piece) {
                is Piece.Offset -> {
                    if (!piece.chained) {
                        base.timeInMillis = now
                        evening = false
                    }
                    applyOffset(base, piece.amount, piece.unit)
                    fireAt = base.timeInMillis
                    previousClock = null
                }

                is Piece.Day -> {
                    val word = piece.word.lowercase()
                    base.timeInMillis = dayFor(word, now)
                    evening = word.startsWith("ton")
                    val preferred = Calendar.getInstance().apply {
                        timeInMillis = base.timeInMillis
                        set(Calendar.HOUR_OF_DAY, if (evening) 20 else 9)
                    }
                    if (preferred.timeInMillis <= now) {
                        preferred.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    fireAt = preferred.timeInMillis
                    previousClock = null
                }

                is Piece.Clock -> {
                    val time = resolveClock(
                        base,
                        now,
                        previousClock,
                        piece.hour,
                        piece.minute,
                        piece.mer,
                        evening,
                    )
                    base.timeInMillis = time
                    fireAt = time
                    previousClock = time
                }
            }
        }

        return fireAt?.takeIf { it > now }
    }

    private fun parseAmount(value: String): Double? =
        value.toDoubleOrNull() ?: NUMBER_WORDS[value.lowercase()]

    private fun withinLimit(amount: Double, unit: String): Boolean {
        if (amount <= 0.0) return false
        return when (unit.firstOrNull()?.lowercaseChar()) {
            'h' -> amount <= MAX_HOURS
            'd' -> amount <= MAX_DAYS
            'w' -> amount <= MAX_WEEKS
            else -> amount <= MAX_MINUTES
        }
    }

    /** Applies an offset to [base], using calendar days and weeks so DST keeps wall time. */
    private fun applyOffset(base: Calendar, amount: Double, unit: String) {
        when (unit.firstOrNull()?.lowercaseChar()) {
            'h' -> base.timeInMillis += (amount * 3_600_000.0).toLong()
            'd' -> addCalendarDays(base, amount)
            'w' -> addCalendarDays(base, amount * 7.0)
            else -> base.timeInMillis += (amount * 60_000.0).toLong()
        }
    }

    private fun addCalendarDays(base: Calendar, amount: Double) {
        val whole = amount.toInt()
        if (whole != 0) base.add(Calendar.DAY_OF_YEAR, whole)
        val fraction = amount - whole
        if (fraction > 0.0) {
            base.timeInMillis += (fraction * DAY_MS).toLong()
        }
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

    private fun resolveClock(
        base: Calendar,
        now: Long,
        previous: Long?,
        hour: Int,
        minute: Int,
        mer: String,
        evening: Boolean,
    ): Long {
        val pm = when {
            mer.startsWith("p") -> true
            mer.startsWith("a") -> false
            evening -> true
            else -> hour !in 8..11
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = base.timeInMillis
            set(Calendar.HOUR_OF_DAY, (hour % 12) + if (pm) 12 else 0)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var time = cal.timeInMillis
        if (time <= now || (previous != null && previous > now && time < previous)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            time = cal.timeInMillis
        }
        return time
    }

    private const val DAY_MS = 24 * 3_600_000L
    private const val MAX_DAYS = 3_650.0
    private const val MAX_WEEKS = MAX_DAYS / 7.0
    private const val MAX_HOURS = MAX_DAYS * 24.0
    private const val MAX_MINUTES = MAX_HOURS * 60.0
}
