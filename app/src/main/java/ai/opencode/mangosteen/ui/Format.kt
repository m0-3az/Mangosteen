package ai.opencode.mangosteen.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compact relative label for the conversation list: now / 4 min / 21:14 / Mon / Aug 3. */
fun relativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000} min"
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
        diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(time))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
    }
}

/** Clock label used by the in-chat separators, e.g. 8:19PM. */
fun clockTime(time: Long): String =
    SimpleDateFormat("h:mma", Locale.US).format(Date(time)).replace("AM", "AM").replace("PM", "PM")

/** Compact token counts: 999 / 12.4k / 1.05M. */
fun formatCount(value: Long): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
    else -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
}

/** Strips the most common markdown markers for list previews. */
fun plainText(input: String): String {
    var s = input
    s = s.replace(Regex("!?\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
    s = s.replace("**", "").replace("__", "").replace("`", "").replace("~~", "")
    s = s.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
    s = s.replace(Regex("\\s+"), " ")
    return s.trim()
}
