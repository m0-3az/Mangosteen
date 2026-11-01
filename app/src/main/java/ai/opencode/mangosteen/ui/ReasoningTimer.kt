package ai.opencode.mangosteen.ui

import android.os.SystemClock

/**
 * Measures how long a reply spends reasoning. The clock starts when the
 * request is sent, so a provider that delivers reasoning in one chunk still
 * reports the first-token latency, and freezes at the first content chunk.
 */
internal class ReasoningTimer(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private var startedAt = 0L
    private var finishedAt = 0L
    private var sawReasoning = false

    fun start() {
        startedAt = clock()
        finishedAt = 0L
        sawReasoning = false
    }

    fun onReasoning() {
        sawReasoning = true
    }

    /** Freezes the clock at the first content chunk and returns the duration. */
    fun onContent(): Long? {
        if (startedAt <= 0L) return null
        if (finishedAt <= 0L) finishedAt = clock()
        return if (sawReasoning) (finishedAt - startedAt).coerceAtLeast(0L) else null
    }

    /** Final duration, or null when the reply never streamed any reasoning. */
    fun finish(): Long? {
        if (!sawReasoning || startedAt <= 0L) return null
        val end = if (finishedAt >= startedAt) finishedAt else clock()
        return (end - startedAt).coerceAtLeast(0L)
    }
}
