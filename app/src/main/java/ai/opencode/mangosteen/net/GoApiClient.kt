package ai.opencode.mangosteen.net

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for the OpenCode Go endpoint. Uses HttpURLConnection and the
 * platform JSON parser so the APK stays free of networking libraries.
 *
 * Auth differs by family: /chat/completions and /responses take a Bearer token,
 * while /messages (Anthropic format) takes x-api-key.
 */
class GoApiClient(private val apiKey: String) {

    companion object {
        const val BASE = "https://opencode.ai/zen/go/v1"
        const val USER_AGENT = "Mangosteen/1.0 (Android; opencode-go)"
        private const val MAX_OUTPUT_TOKENS = 8192
    }

    @Volatile
    private var active: HttpURLConnection? = null

    /** Aborts the in-flight request, if any. */
    fun cancel() {
        runCatching { active?.disconnect() }
        active = null
    }

    fun listModels(): List<String> {
        val conn = open("$BASE/models", "GET", bearerHeaders(null))
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw failure(conn, code)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(body).getJSONArray("data")
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) out.add(arr.getJSONObject(i).getString("id"))
            return out
        } finally {
            active = null
        }
    }

    /**
     * Sends [history] (must already include the latest user turn) and streams the
     * reply. [onDelta] is called with (contentChunk, reasoningChunk); exactly one of
     * the two is non-empty per call.
     *
     * If the provider rejects an optional parameter the request is retried with
     * just that parameter dropped - search first, and only as a last resort
     * thinking - so a rejected web search can never silently re-enable reasoning.
     * [onNotice] reports any downgrade to the caller.
     */
    fun stream(
        model: ModelInfo,
        systemPrompt: String?,
        history: List<ChatMessage>,
        sessionId: String,
        options: RequestOptions,
        onNotice: ((String) -> Unit)? = null,
        onDelta: (content: String, reasoning: String) -> Unit
    ): ChatMessage {
        // Try the request as asked; on a 400 progressively drop optional
        // parameters (search first, then reasoning preferences) so a provider
        // that rejects them can never make the chat unusable.
        val candidates = buildList {
            add(options)
            if (options.search) add(options.copy(search = false))
            if (options.reasoning == Reasoning.LOW || options.reasoning == Reasoning.HIGH) {
                add(options.copy(reasoning = Reasoning.ON, search = false))
            }
            if (options.reasoning == Reasoning.OFF) {
                add(options.copy(reasoning = Reasoning.ON, search = false))
            }
            add(RequestOptions(reasoning = Reasoning.ON, search = false))
        }.distinct()

        var lastError: ApiException? = null
        for (i in candidates.indices) {
            val candidate = candidates[i]
            try {
                return dispatch(model, systemPrompt, history, sessionId, candidate, onDelta)
            } catch (error: ApiException) {
                lastError = error
                if (error.code != 400) throw error
                val next = candidates.getOrNull(i + 1) ?: break
                if (candidate.search && !next.search) {
                    onNotice?.invoke("Search isn't available for this model")
                }
                if (candidate.reasoning != next.reasoning && next.reasoning == Reasoning.ON) {
                    onNotice?.invoke(
                        if (candidate.reasoning == Reasoning.OFF) {
                            "Thinking can't be disabled for this model"
                        } else {
                            "Reasoning levels aren't available for this model"
                        }
                    )
                }
            }
        }
        throw lastError ?: ApiException("Request failed")
    }

    private fun dispatch(
        model: ModelInfo,
        systemPrompt: String?,
        history: List<ChatMessage>,
        sessionId: String,
        options: RequestOptions,
        onDelta: (content: String, reasoning: String) -> Unit
    ): ChatMessage = when (model.protocol) {
        Protocol.CHAT -> streamChat(model.id, systemPrompt, history, sessionId, options, onDelta)
        Protocol.MESSAGES -> streamMessages(model.id, systemPrompt, history, sessionId, options, onDelta)
        Protocol.RESPONSES -> streamResponses(model.id, systemPrompt, history, sessionId, options, onDelta)
    }

    // ---------------------------------------------------------------- chat

    private fun streamChat(
        modelId: String,
        systemPrompt: String?,
        history: List<ChatMessage>,
        sessionId: String,
        options: RequestOptions,
        onDelta: (String, String) -> Unit
    ): ChatMessage {
        val messages = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        history.forEach {
            messages.put(JSONObject().put("role", it.role).put("content", chatContent(it)))
        }
        val payload = JSONObject()
            .put("model", modelId)
            .put("stream", true)
            .put("stream_options", JSONObject().put("include_usage", true))
            .put("messages", messages)
        when (options.reasoning) {
            Reasoning.OFF -> payload.put("reasoning_effort", "none")
            Reasoning.LOW -> payload.put("reasoning_effort", "low")
            Reasoning.HIGH -> payload.put("reasoning_effort", "high")
            Reasoning.ON -> Unit
        }

        val conn = post("$BASE/chat/completions", payload, bearerHeaders(sessionId))
        verify(conn)

        val content = StringBuilder()
        val reasoning = StringBuilder()
        var usage: Usage? = null
        readSse(conn) { data ->
            if (data == "[DONE]") return@readSse true
            val obj = runCatching { JSONObject(data) }.getOrNull() ?: return@readSse false
            obj.optJSONObject("usage")?.let { usage = usageOf(it) }
            val choices = obj.optJSONArray("choices") ?: return@readSse false
            if (choices.length() == 0) return@readSse false
            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return@readSse false
            delta.optStr("content")?.let { content.append(it); onDelta(it, "") }
            delta.optStr("reasoning_content")?.let { reasoning.append(it); onDelta("", it) }
            false
        }
        return finish(content, reasoning, usage)
    }

    // ------------------------------------------------------------ messages

    private fun streamMessages(
        modelId: String,
        systemPrompt: String?,
        history: List<ChatMessage>,
        sessionId: String,
        options: RequestOptions,
        onDelta: (String, String) -> Unit
    ): ChatMessage {
        val messages = JSONArray()
        history.forEach {
            messages.put(JSONObject().put("role", it.role).put("content", anthropicContent(it)))
        }
        val payload = JSONObject()
            .put("model", modelId)
            .put("max_tokens", MAX_OUTPUT_TOKENS)
            .put("stream", true)
            .put("messages", messages)
        if (!systemPrompt.isNullOrBlank()) payload.put("system", systemPrompt)
        if (options.reasoning == Reasoning.OFF) {
            payload.put("thinking", JSONObject().put("type", "disabled"))
        }
        if (options.search) {
            payload.put(
                "tools",
                JSONArray().put(
                    JSONObject()
                        .put("type", "web_search_20250305")
                        .put("name", "web_search")
                        .put("max_uses", 5)
                )
            )
        }

        val conn = post("$BASE/messages", payload, anthropicHeaders(sessionId))
        verify(conn)

        val content = StringBuilder()
        val reasoning = StringBuilder()
        var usage: Usage? = null
        readSse(conn) { data ->
            val obj = runCatching { JSONObject(data) }.getOrNull() ?: return@readSse false
            when (obj.optString("type")) {
                "content_block_delta" -> {
                    val delta = obj.optJSONObject("delta") ?: return@readSse false
                    when (delta.optString("type")) {
                        "text_delta" -> delta.optStr("text")?.let { content.append(it); onDelta(it, "") }
                        "thinking_delta" -> delta.optStr("thinking")?.let { reasoning.append(it); onDelta("", it) }
                    }
                }
                "message_delta" -> obj.optJSONObject("usage")?.let { usage = usageOf(it) }
                "message_stop" -> return@readSse true
            }
            false
        }
        return finish(content, reasoning, usage)
    }

    // ----------------------------------------------------------- responses

    private fun streamResponses(
        modelId: String,
        systemPrompt: String?,
        history: List<ChatMessage>,
        sessionId: String,
        options: RequestOptions,
        onDelta: (String, String) -> Unit
    ): ChatMessage {
        val input = JSONArray()
        history.forEach {
            input.put(JSONObject().put("role", it.role).put("content", responsesContent(it)))
        }
        val payload = JSONObject()
            .put("model", modelId)
            .put("stream", true)
            .put("max_output_tokens", MAX_OUTPUT_TOKENS)
            .put("input", input)
        if (!systemPrompt.isNullOrBlank()) payload.put("instructions", systemPrompt)
        when (options.reasoning) {
            Reasoning.OFF -> payload.put("reasoning", JSONObject().put("effort", "none"))
            Reasoning.LOW -> payload.put("reasoning", JSONObject().put("effort", "low"))
            Reasoning.HIGH -> payload.put("reasoning", JSONObject().put("effort", "high"))
            Reasoning.ON -> Unit
        }
        if (options.search) {
            payload.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
        }

        val conn = post("$BASE/responses", payload, bearerHeaders(sessionId))
        verify(conn)

        val content = StringBuilder()
        val reasoning = StringBuilder()
        var usage: Usage? = null
        readSse(conn) { data ->
            val obj = runCatching { JSONObject(data) }.getOrNull() ?: return@readSse false
            when (obj.optString("type")) {
                "response.output_text.delta" ->
                    obj.optStr("delta")?.let { content.append(it); onDelta(it, "") }
                "response.reasoning_summary_text.delta", "response.reasoning_text.delta" ->
                    obj.optStr("delta")?.let { reasoning.append(it); onDelta("", it) }
                "response.completed" -> {
                    obj.optJSONObject("response")?.optJSONObject("usage")?.let { usage = usageOf(it) }
                    return@readSse true
                }
                "response.failed", "response.incomplete" -> return@readSse true
            }
            false
        }
        return finish(content, reasoning, usage)
    }

    // ------------------------------------------------------------- plumbing

    /** Content for /chat/completions: a plain string, or text + image parts. */
    private fun chatContent(message: ChatMessage): Any {
        val image = message.imageBase64 ?: return message.content
        val parts = JSONArray()
        if (message.content.isNotBlank()) {
            parts.put(JSONObject().put("type", "text").put("text", message.content))
        }
        parts.put(
            JSONObject()
                .put("type", "image_url")
                .put("image_url", JSONObject().put("url", dataUrl(message, image)))
        )
        return parts
    }

    /** Content for Anthropic /messages: text + base64 image source blocks. */
    private fun anthropicContent(message: ChatMessage): Any {
        val image = message.imageBase64 ?: return message.content
        val parts = JSONArray()
        if (message.content.isNotBlank()) {
            parts.put(JSONObject().put("type", "text").put("text", message.content))
        }
        parts.put(
            JSONObject()
                .put("type", "image")
                .put(
                    "source",
                    JSONObject()
                        .put("type", "base64")
                        .put("media_type", message.imageMime ?: "image/jpeg")
                        .put("data", image)
                )
        )
        return parts
    }

    /** Content for /responses: input_text + input_image parts. */
    private fun responsesContent(message: ChatMessage): Any {
        val image = message.imageBase64 ?: return message.content
        val parts = JSONArray()
        if (message.content.isNotBlank()) {
            parts.put(JSONObject().put("type", "input_text").put("text", message.content))
        }
        parts.put(
            JSONObject()
                .put("type", "input_image")
                .put("image_url", dataUrl(message, image))
        )
        return parts
    }

    private fun dataUrl(message: ChatMessage, base64: String): String =
        "data:${message.imageMime ?: "image/jpeg"};base64,$base64"

    private fun usageOf(json: JSONObject): Usage {
        val prompt = json.optInt("prompt_tokens", json.optInt("input_tokens", 0))
        val completion = json.optInt("completion_tokens", json.optInt("output_tokens", 0))
        val total = json.optInt("total_tokens", prompt + completion)
        return Usage(prompt, completion, total)
    }

    private fun finish(content: StringBuilder, reasoning: StringBuilder, usage: Usage?): ChatMessage =
        ChatMessage(
            role = "assistant",
            content = content.toString(),
            reasoning = reasoning.toString().ifEmpty { null },
            usage = usage
        )

    private fun readSse(conn: HttpURLConnection, onData: (String) -> Boolean) {
        BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.substring(5).trim()
                if (data.isEmpty()) continue
                if (onData(data)) break
            }
        }
    }

    private fun open(url: String, method: String, headers: Map<String, String>): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 20_000
        conn.readTimeout = 300_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "text/event-stream, application/json")
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        active = conn
        return conn
    }

    private fun post(url: String, payload: JSONObject, headers: Map<String, String>): HttpURLConnection {
        val conn = open(url, "POST", headers)
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        return conn
    }

    private fun verify(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code !in 200..299) throw failure(conn, code)
    }

    private fun bearerHeaders(session: String?): Map<String, String> = buildMap {
        put("Authorization", "Bearer $apiKey")
        if (session != null) put("x-opencode-session", session)
    }

    private fun anthropicHeaders(session: String?): Map<String, String> = buildMap {
        put("x-api-key", apiKey)
        put("anthropic-version", "2023-06-01")
        if (session != null) put("x-opencode-session", session)
    }

    private fun failure(conn: HttpURLConnection, code: Int): ApiException {
        val raw = runCatching {
            (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        val serverMsg = runCatching {
            val o = JSONObject(raw)
            o.optJSONObject("error")?.optString("message")?.ifEmpty { null }
                ?: o.optString("message").ifEmpty { null }
        }.getOrNull()
        val friendly = when (code) {
            401 -> "Invalid or missing API key."
            402 -> "Usage limit reached on your OpenCode Go plan."
            404 -> "Model not available on your plan."
            429 -> "Rate limited. Try again in a moment."
            else -> serverMsg ?: "Request failed (HTTP $code)"
        }
        return ApiException(friendly, code)
    }
}
