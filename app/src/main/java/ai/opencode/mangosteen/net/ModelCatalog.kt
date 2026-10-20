package ai.opencode.mangosteen.net

/**
 * Maps OpenCode Go model ids to the protocol family each one speaks, and
 * provides display names. The three families were verified against the live
 * endpoint: /chat/completions, /messages (Anthropic), /responses (OpenAI).
 */
object ModelCatalog {

    private val MESSAGES = setOf(
        "minimax-m3", "minimax-m2.7", "minimax-m2.5",
        "qwen3.8-max", "qwen3.8-flash", "qwen3.7-max", "qwen3.7-plus",
        "qwen3.6-plus", "qwen3.5-plus", "union-alpha"
    )

    private val RESPONSES = setOf(
        "grok-4.6", "grok-4.5", "gpt-5.6-luna",
        "muse-spark-1.3-contributor", "muse-spark-1.2-contributor", "omen-alpha"
    )

    private val FAVORITES = listOf(
        "deepseek-v4.1-flash", "deepseek-v4-pro", "glm-5.3", "kimi-k3",
        "minimax-m3", "qwen3.8-flash", "grok-4.6", "gpt-5.6-luna"
    )

    /** Used when /models can't be reached (e.g. first run offline). */
    val FALLBACK = listOf(
        "deepseek-v4.1-flash", "deepseek-v4-pro", "deepseek-v4-flash",
        "glm-5.3", "glm-5.2", "glm-5.1", "kimi-k3", "kimi-k2.7-code",
        "minimax-m3", "qwen3.8-flash", "grok-4.6", "gpt-5.6-luna",
        "longcat-2.0", "mimo-v2.5", "hy3"
    )

    const val DEFAULT_MODEL = "deepseek-v4.1-flash"
    private const val DEFAULT_CONTEXT = 128_000

    /**
     * Context window sizes, taken from the provider metadata exposed by
     * `opencode models --verbose`.
     */
    private val CONTEXT = mapOf(
        "deepseek-v4-flash" to 1_000_000,
        "deepseek-v4-flash-vision-exp" to 1_000_000,
        "deepseek-v4-pro" to 1_000_000,
        "deepseek-v4.1-flash" to 1_000_000,
        "glm-5" to 128_000,
        "glm-5.1" to 202_752,
        "glm-5.2" to 1_000_000,
        "glm-5.3" to 1_000_000,
        "glm-5.3-flash" to 1_000_000,
        "gpt-5.6-luna" to 1_050_000,
        "grok-4.5" to 500_000,
        "grok-4.6" to 500_000,
        "hy3" to 256_000,
        "hy3-preview" to 256_000,
        "hy4-preview" to 1_024_000,
        "kimi-k2.5" to 262_144,
        "kimi-k2.6" to 262_144,
        "kimi-k2.7-code" to 262_144,
        "kimi-k3" to 1_048_576,
        "longcat-2.0" to 1_000_000,
        "mimo-v2.5" to 1_000_000,
        "mimo-v2.5-pro" to 1_048_576,
        "mimo-v2-pro" to 128_000,
        "mimo-v2-omni" to 128_000,
        "minimax-m2.5" to 204_800,
        "minimax-m2.7" to 204_800,
        "minimax-m3" to 1_000_000,
        "muse-spark-1.2-contributor" to 1_048_576,
        "muse-spark-1.3-contributor" to 1_048_576,
        "qwen3.5-plus" to 1_000_000,
        "qwen3.6-plus" to 1_000_000,
        "qwen3.7-max" to 1_000_000,
        "qwen3.7-plus" to 1_000_000,
        "qwen3.8-flash" to 1_000_000,
        "qwen3.8-max" to 1_000_000,
        "union-alpha" to 262_144
    )

    fun contextWindow(id: String): Int = CONTEXT[id] ?: DEFAULT_CONTEXT

    /**
     * Best-effort guess at whether a model accepts image input. The provider
     * still has the final word; this only drives a heads-up before sending.
     */
    fun visionSupported(id: String): Boolean {
        if (id.contains("vision")) return true
        return id.startsWith("gpt-") ||
            id.startsWith("grok-") ||
            id.startsWith("qwen") ||
            id.startsWith("glm-") ||
            id.startsWith("kimi-") ||
            id.startsWith("minimax-") ||
            id.startsWith("muse-")
    }

    /**
     * Whether the provider accepts a web-search tool for this model. Verified
     * against the live endpoint; unsupported models return 400/403.
     */
    fun searchSupported(id: String): Boolean = when (protocolFor(id)) {
        Protocol.CHAT -> false
        Protocol.MESSAGES -> id != "union-alpha"
        Protocol.RESPONSES -> id in setOf("grok-4.6", "grok-4.5", "gpt-5.6-luna")
    }

    fun protocolFor(id: String): Protocol = when {
        id in MESSAGES -> Protocol.MESSAGES
        id in RESPONSES -> Protocol.RESPONSES
        else -> Protocol.CHAT
    }

    fun info(id: String) = ModelInfo(id, protocolFor(id), prettify(id))

    fun sorted(ids: List<String>): List<ModelInfo> {
        val favIdx = FAVORITES.withIndex().associate { it.value to it.index }
        return ids.distinct().map { info(it) }
            .sortedWith(compareBy({ favIdx[it.id] ?: Int.MAX_VALUE }, { it.id }))
    }

    private val SPECIAL = mapOf(
        "deepseek-v4.1-flash" to "DeepSeek V4.1 Flash",
        "deepseek-v4-pro" to "DeepSeek V4 Pro",
        "deepseek-v4-flash" to "DeepSeek V4 Flash",
        "deepseek-v4-flash-vision-exp" to "DeepSeek V4 Flash Vision",
        "glm-5.3-flash" to "GLM-5.3 Flash",
        "glm-5.3" to "GLM-5.3",
        "gpt-5.6-luna" to "GPT-5.6 Luna",
        "mimo-v2.5" to "MiMo V2.5",
        "mimo-v2.5-pro" to "MiMo V2.5 Pro",
        "mimo-v2-pro" to "MiMo V2 Pro",
        "mimo-v2-omni" to "MiMo V2 Omni",
        "hy3" to "Hy3",
        "hy4-preview" to "Hy4 Preview",
        "hy3-preview" to "Hy3 Preview",
        "kimi-k2.7-code" to "Kimi K2.7 Code",
        "kimi-k2.6" to "Kimi K2.6",
        "kimi-k2.5" to "Kimi K2.5",
        "kimi-k3" to "Kimi K3",
        "minimax-m3" to "MiniMax M3",
        "minimax-m2.7" to "MiniMax M2.7",
        "minimax-m2.5" to "MiniMax M2.5",
        "qwen3.8-max" to "Qwen3.8 Max",
        "qwen3.8-flash" to "Qwen3.8 Flash",
        "qwen3.7-max" to "Qwen3.7 Max",
        "qwen3.7-plus" to "Qwen3.7 Plus",
        "longcat-2.0" to "LongCat 2.0",
        "union-alpha" to "Union Alpha (free)",
        "grok-4.6" to "Grok 4.6",
        "grok-4.5" to "Grok 4.5",
        "omen-alpha" to "Omen Alpha",
        "muse-spark-1.3-contributor" to "Muse Spark 1.3",
        "muse-spark-1.2-contributor" to "Muse Spark 1.2"
    )

    fun prettify(id: String): String = SPECIAL[id] ?: id.split('-').joinToString(" ") { w ->
        if (w.isEmpty()) w else w[0].uppercase() + w.substring(1)
    }
}
