package ai.opencode.mangosteen.net

import org.json.JSONObject

/** Which wire protocol a model speaks. */
enum class Protocol { CHAT, MESSAGES, RESPONSES }

data class ModelInfo(
    val id: String,
    val protocol: Protocol,
    val display: String
)

/** How much reasoning the model may do. ON leaves the provider default. */
enum class Reasoning { OFF, ON, LOW, HIGH }

/** Per-request toggles surfaced as chips in the composer. */
data class RequestOptions(
    val reasoning: Reasoning = Reasoning.ON,
    val search: Boolean = false
)

/** Token accounting for one completed turn. */
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/** A single conversational turn sent to the API. */
data class ChatMessage(
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val usage: Usage? = null,
    val imageBase64: String? = null,
    val imageMime: String? = "image/jpeg"
)

class ApiException(message: String, val code: Int? = null) : Exception(message)

internal fun JSONObject.optStr(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val s = getString(key)
    return s.ifEmpty { null }
}
