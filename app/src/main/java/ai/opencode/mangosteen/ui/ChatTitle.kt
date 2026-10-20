package ai.opencode.mangosteen.ui

/**
 * One-shot request that names a chat from its opening exchange, plus the
 * cleanup for whatever the model answers with.
 */
object ChatTitle {

    const val INSTRUCTION =
        "You name chat threads. Reply with only a title of 2 to 5 words, in the same " +
            "language as the conversation. No quotes, no trailing punctuation, nothing else."

    fun request(firstUserMessage: String, firstReply: String): String =
        "user: ${firstUserMessage.take(1000)}\n\nassistant: ${firstReply.take(1000)}"

    /** First usable line of a model reply, stripped of fences, quotes and a "Title:" prefix. */
    fun clean(raw: String): String? {
        val stripped = raw.trim().removePrefix("```").removeSuffix("```").trim()
        val line = stripped.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: return null
        val cleaned = line
            .trim('"', '\'', '“', '”', '‘', '’')
            .replace(Regex("^title\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .trimEnd('.', '!', '?', ':', ';', ',')
            .trim()
            .trim('"', '\'', '“', '”', '‘', '’')
            .trim()
        return cleaned.take(48).ifBlank { null }
    }
}
