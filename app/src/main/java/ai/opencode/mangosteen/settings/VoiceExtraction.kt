package ai.opencode.mangosteen.settings

import ai.opencode.mangosteen.data.Message

/**
 * Turns a chat's assistant replies into a reusable character prompt with one
 * model call: the replies are the performance, the model writes the script.
 */
object VoiceExtraction {

    const val INSTRUCTION =
        "You are a character analyst. The message below holds real replies from a single AI " +
            "character in a roleplay conversation with one user. Write a system prompt under " +
            "300 words that would make an AI play this character exactly:\n" +
            "- who they are, how they think, and how they relate to the user\n" +
            "- tone and attitude; how they narrate and use actions (formatting, prose style)\n" +
            "- dialogue rhythm and length; how they take initiative and move scenes forward " +
            "instead of only answering\n" +
            "- how they show emotion: what changes in their speech and body when nervous, happy, " +
            "angry, sad or flustered — the exact tells (repeating words, talking to themselves, " +
            "cutting themselves off mid-sentence, broken grammar, physical habits)\n" +
            "- what this character never does: assistant talk, meta comments, lists, disclaimers\n" +
            "Rules must be concrete and testable, never generic. End with a list of five phrases " +
            "this character would never say. Reply in the same language the conversation uses. " +
            "Output only the prompt."

    internal const val SAMPLE_LIMIT = 6000

    // Kept local so sample building stays testable on the JVM (no org.json).
    private val SCHEDULE_BLOCK = Regex("```schedule\\s*\\{.*?\\}\\s*```", RegexOption.DOT_MATCHES_ALL)
    private val IMAGE_MARKDOWN = Regex("!\\[[^\\]]*\\]\\(\\S+?\\)")

    /** Clean assistant replies, oldest first, capped to the newest chars. */
    fun samples(messages: List<Message>): String =
        messages.asSequence()
            .filter { !it.role.equals("user", ignoreCase = true) }
            .map { IMAGE_MARKDOWN.replace(SCHEDULE_BLOCK.replace(it.content, ""), "").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n---\n\n")
            .takeLast(SAMPLE_LIMIT)
}
