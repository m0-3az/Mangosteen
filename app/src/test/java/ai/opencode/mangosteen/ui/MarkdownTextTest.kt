package ai.opencode.mangosteen.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {

    private val codeBg = Color(0xFF222222)
    private val link = Color(0xFF4ECDC4)

    private fun blocks(text: String) = parseMarkdown(text, codeBg, link)

    @Test
    fun `heading does not swallow the following paragraph`() {
        val result = blocks("# Release notes\nFixed **markdown** rendering.")

        assertEquals(2, result.size)
        assertEquals("Release notes", (result[0] as MdBlock.Heading).text.text)
        assertEquals(1, (result[0] as MdBlock.Heading).level)
        assertEquals("Fixed markdown rendering.", (result[1] as MdBlock.Paragraph).text.text)
    }

    @Test
    fun `lists work without a blank line and keep nesting`() {
        val result = blocks("Intro\n- one\n  - child\n3. third")

        assertEquals(4, result.size)
        assertTrue(result[0] is MdBlock.Paragraph)
        val first = result[1] as MdBlock.Bullet
        val child = result[2] as MdBlock.Bullet
        assertEquals(0, first.level)
        assertEquals("one", first.text.text)
        assertEquals(1, child.level)
        assertEquals(3, (result[3] as MdBlock.Numbered).index)
    }

    @Test
    fun `quote and fenced code are separate blocks`() {
        val result = blocks("> quoted **text**\n\n```kotlin\n# not a heading\nprintln(\"hi\")\n```")

        assertEquals(2, result.size)
        assertEquals("quoted text", (result[0] as MdBlock.Quote).text.text)
        val code = result[1] as MdBlock.Code
        assertEquals("kotlin", code.language)
        assertEquals("# not a heading\nprintln(\"hi\")", code.code)
    }

    @Test
    fun `inline markers are removed and links keep their label`() {
        val result = blocks(
            "Use `code`, **bold**, *italic*, ~~gone~~, ==mark==, [site](https://example.com)."
        )

        assertEquals(
            "Use code, bold, italic, gone, mark, site.",
            (result.single() as MdBlock.Paragraph).text.text,
        )
    }

    @Test
    fun `horizontal rule is its own block`() {
        val result = blocks("above\n\n---\n\nbelow")

        assertEquals(3, result.size)
        assertEquals("above", (result[0] as MdBlock.Paragraph).text.text)
        assertTrue(result[1] is MdBlock.Rule)
        assertEquals("below", (result[2] as MdBlock.Paragraph).text.text)
    }

    @Test
    fun `empty and malformed markdown never throws`() {
        assertTrue(blocks("").isEmpty())
        assertTrue(blocks("\r\n\r\n").isEmpty())
        assertEquals(1, blocks("```kotlin\nunclosed fence").size)
        assertEquals(1, blocks("[broken](").size)
        assertEquals(1, blocks("*_~`").size)
        assertEquals(1, blocks("###").size)
        assertEquals(1, blocks("- ").size)
    }
}
