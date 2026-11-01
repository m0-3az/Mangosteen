package ai.opencode.mangosteen.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
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
    fun `four backtick fence keeps inner triple fences as code`() {
        val result = blocks(
            "````markdown\n" +
                "```kotlin\n" +
                "println(\"hi\")\n" +
                "```\n" +
                "````"
        )

        val code = result.single() as MdBlock.Code
        assertEquals("markdown", code.language)
        assertEquals("```kotlin\nprintln(\"hi\")\n```", code.code)
    }

    @Test
    fun `fence indented under a list item becomes code`() {
        val result = blocks("- item\n    ```kotlin\n    val x = 1\n    ```")

        assertEquals(2, result.size)
        assertEquals("item", (result[0] as MdBlock.Bullet).text.text)
        val code = result[1] as MdBlock.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun `repeated one markers count up`() {
        val result = blocks("1. one\n1. two\n1. three")

        assertEquals(listOf(1, 2, 3), result.map { (it as MdBlock.Numbered).index })
    }

    @Test
    fun `only an ordered marker of one interrupts a paragraph`() {
        val result = blocks("Notes\n2024. It went well.\nStill prose")

        assertEquals(1, result.size)
        assertEquals(
            "Notes\n2024. It went well.\nStill prose",
            (result.single() as MdBlock.Paragraph).text.text,
        )
    }

    @Test
    fun `wrapped bullet text stays in the bullet`() {
        val result = blocks("- one\nwrapped text")

        assertEquals(1, result.size)
        assertEquals("one\nwrapped text", (result.single() as MdBlock.Bullet).text.text)
    }

    @Test
    fun `wrapped quote text stays in the quote`() {
        val result = blocks("> quote\ncontinued")

        assertEquals(1, result.size)
        assertEquals("quote\ncontinued", (result.single() as MdBlock.Quote).text.text)
    }

    @Test
    fun `link destination keeps balanced parentheses`() {
        val result = blocks("[wiki](https://en.wikipedia.org/wiki/Foo_(bar))")
        val text = (result.single() as MdBlock.Paragraph).text

        assertEquals("wiki", text.text)
        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        assertEquals("https://en.wikipedia.org/wiki/Foo_(bar)", link.url)
    }

    @Test
    fun `inline link exposes its url`() {
        val result = blocks("Open [site](https://example.com) now")
        val text = (result.single() as MdBlock.Paragraph).text

        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url
        assertEquals("https://example.com", link.url)
        assertEquals("Open site now", text.text)
    }

    @Test
    fun `heading keeps a trailing hash without a space`() {
        val result = blocks("# C#")

        assertEquals("C#", (result.single() as MdBlock.Heading).text.text)
    }

    @Test
    fun `multiplication asterisks are not italics`() {
        val result = blocks("2 * 3 * 4")

        assertEquals("2 * 3 * 4", (result.single() as MdBlock.Paragraph).text.text)
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
