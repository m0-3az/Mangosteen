package ai.opencode.mangosteen.settings

import ai.opencode.mangosteen.data.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceExtractionTest {

    @Test
    fun `keeps assistant replies only and strips schedule blocks and images`() {
        val samples = VoiceExtraction.samples(
            listOf(
                message("user", "hey, where are you?"),
                message(
                    "assistant",
                    "By the door. I— I've been waiting.\n\n```schedule\n" +
                        "{\"at\":\"2026-10-16T09:00\",\"note\":\"ask how it went\"}\n```",
                ),
                message("assistant", "![photo](file:///sdcard/img.jpg) You came."),
            )
        )

        assertEquals("By the door. I— I've been waiting.\n\n---\n\nYou came.", samples)
    }

    @Test
    fun `keeps the newest part when samples are long`() {
        val long = "a".repeat(VoiceExtraction.SAMPLE_LIMIT + 500) + "tail"
        val samples = VoiceExtraction.samples(listOf(message("assistant", long)))

        assertEquals(VoiceExtraction.SAMPLE_LIMIT, samples.length)
        assertTrue(samples.endsWith("tail"))
    }

    @Test
    fun `blank replies leave nothing`() {
        val samples = VoiceExtraction.samples(
            listOf(message("assistant", "   "), message("user", "hi"))
        )

        assertTrue(samples.isBlank())
    }

    private fun message(role: String, content: String) =
        Message(0L, 1L, role, content, null, null, 0L)
}
