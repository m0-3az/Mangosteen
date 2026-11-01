package ai.opencode.mangosteen.proactive

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Teaches the model that it is allowed to schedule a future check-in, and how
 * to tell the app about it. Appended to the system prompt for every request so
 * the capability is always available without persisting it into the user's
 * own system prompt.
 */
object ProactivePrompt {

    fun instruction(): String {
        val now = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mma", Locale.US).format(Date())
        return buildString {
            append("You can also message the user later, on your own, without them prompting you. ")
            append("When they mention a near-future event or appointment (an exam, a meeting, a trip, ")
            append("a deadline) or ask to be checked on, schedule a follow-up. To do that, end your ")
            append("reply with exactly one block on its own line and nothing after it:\n\n")
            append("```schedule\n")
            append("{\"at\":\"YYYY-MM-DDTHH:MM\",\"note\":\"private reminder of what to follow up on\"}\n")
            append("```\n\n")
            append("Use the user's local time and pick a moment just after the event. If they use a ")
            append("relative phrase such as \"in two hours\" or \"after 10 minutes\", convert it to ")
            append("the matching local wall-clock time in the block. The note is for ")
            append("you, never shown to the user. Only schedule when a follow-up genuinely helps. ")
            append("Never mention or explain the block. ")
            append("Current local date and time: $now.")
        }
    }
}
