package ai.opencode.mangosteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

sealed interface MdBlock {
    data class Paragraph(val text: AnnotatedString) : MdBlock
    data class Heading(val level: Int, val text: AnnotatedString) : MdBlock
    data class Bullet(val level: Int, val marker: String, val text: AnnotatedString) : MdBlock
    data class Numbered(val level: Int, val index: Int, val text: AnnotatedString) : MdBlock
    data class Quote(val text: AnnotatedString) : MdBlock
    data class Code(val language: String?, val code: String) : MdBlock
    data object Rule : MdBlock
}

private val HEADING = Regex("""^[ \t]{0,3}(#{1,6})[ \t]+(.*?)(?:[ \t]+#+)?[ \t]*$""")
private val BULLET = Regex("""^([ \t]*)([-*+•])[ \t]+(.*)$""")
private val NUMBERED = Regex("""^([ \t]*)(\d{1,9})[.)][ \t]+(.*)$""")
private val QUOTE = Regex("""^[ \t]{0,3}>[ \t]?(.*)$""")
private val RULE = Regex("""^[ \t]{0,3}([-*_])[ \t]*(?:\1[ \t]*){2,}$""")

/** A fenced code marker: the fence may be indented under a list item. */
private data class FenceMarker(
    val indent: Int,
    val char: Char,
    val length: Int,
    val language: String?,
)

private fun fenceOpen(line: String): FenceMarker? {
    val indent = line.indexOfFirst { it != ' ' && it != '\t' }
    if (indent < 0) return null
    val char = line[indent]
    if (char != '`' && char != '~') return null
    var end = indent
    while (end < line.length && line[end] == char) end++
    val length = end - indent
    if (length < 3) return null
    val info = line.substring(end).trim()
    if (char == '`' && info.contains('`')) return null
    return FenceMarker(indent, char, length, info.ifBlank { null })
}

/** A closing fence is marker-only, the same character, and at least as long. */
private fun fenceCloses(line: String, marker: FenceMarker): Boolean {
    val start = line.indexOfFirst { it != ' ' && it != '\t' }
    if (start < 0 || line[start] != marker.char) return false
    var end = start
    while (end < line.length && line[end] == marker.char) end++
    return end - start >= marker.length && line.substring(end).isBlank()
}

private fun stripIndent(line: String, width: Int): String {
    var end = 0
    while (end < line.length && end < width && (line[end] == ' ' || line[end] == '\t')) end++
    return line.substring(end)
}

/**
 * Inline markup ordered so that code spans win over everything else and links
 * are consumed before bold/italic markers. Group numbers are used below.
 */
private val INLINE = Regex(
    "`([^`\\n]+)`" +
        "|\\[([^\\]]+)\\]\\(((?:[^()\\s]|\\([^()\\s]*\\))+)\\)" +
        "|\\*\\*\\*(.+?)\\*\\*\\*" +
        "|___(.+?)___" +
        "|\\*\\*(.+?)\\*\\*" +
        "|__(.+?)__" +
        "|~~(.+?)~~" +
        "|==(.+?)==" +
        "|\\*([^\\s*](?:[^*\\n]*[^\\s*])?)\\*" +
        "|(?<![\\w_])_([^\\s_](?:[^_\\n]*[^\\s_])?)_(?![\\w_])" +
        "|<((?:https?://|mailto:)[^>\\s]+)>"
)

/**
 * Model output is untrusted, so only ordinary web and mail links may become
 * tappable. The check is case insensitive and requires `//` for HTTP, keeping
 * javascript:, intent:, file:, content: and data: destinations as plain text.
 */
private val SAFE_LINK_PREFIXES = listOf("https://", "http://", "mailto:")

private fun isSafeLinkDestination(url: String): Boolean {
    val lower = url.lowercase(Locale.ROOT)
    return SAFE_LINK_PREFIXES.any(lower::startsWith)
}

private fun indentWidth(prefix: String): Int = prefix.length

private fun levelFor(indent: Int): Int = (indent / 2).coerceIn(0, 6)

private fun isBlockStart(line: String): Boolean =
    line.isBlank() ||
        fenceOpen(line) != null ||
        HEADING.matchEntire(line) != null ||
        RULE.matchEntire(line) != null ||
        QUOTE.matchEntire(line) != null ||
        BULLET.matchEntire(line) != null ||
        NUMBERED.matchEntire(line) != null

/** Only an ordered marker of one may interrupt an existing paragraph. */
private fun isParagraphBreak(line: String): Boolean {
    if (!isBlockStart(line)) return false
    val numbered = NUMBERED.matchEntire(line) ?: return true
    return numbered.groupValues[2].toIntOrNull() == 1
}

/**
 * Parses the Markdown flavours models actually emit. The parser is deliberately
 * line based: a heading is always one block, lists stop when a non-list line
 * appears, and fenced code is copied verbatim. Previously headings swallowed
 * the paragraph after them and lists only worked when preceded by a blank line.
 */
internal fun parseMarkdown(text: String, codeBg: Color, link: Color): List<MdBlock> {
    val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines()
    val out = ArrayList<MdBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        if (line.isBlank()) {
            i++
            continue
        }

        val fence = fenceOpen(line)
        if (fence != null) {
            val code = ArrayList<String>()
            i++
            while (i < lines.size) {
                if (fenceCloses(lines[i], fence)) {
                    i++
                    break
                }
                code.add(stripIndent(lines[i], fence.indent))
                i++
            }
            out.add(MdBlock.Code(fence.language, code.joinToString("\n")))
            continue
        }

        val heading = HEADING.matchEntire(line)
        if (heading != null) {
            out.add(
                MdBlock.Heading(
                    level = heading.groupValues[1].length,
                    text = annotate(heading.groupValues[2].trim(), codeBg, link),
                )
            )
            i++
            continue
        }

        if (RULE.matches(line)) {
            out.add(MdBlock.Rule)
            i++
            continue
        }

        if (QUOTE.matchEntire(line) != null) {
            val quoted = ArrayList<String>()
            while (i < lines.size) {
                val candidate = lines[i]
                val match = QUOTE.matchEntire(candidate)
                if (match != null) {
                    quoted.add(match.groupValues[1].trimEnd())
                    i++
                    continue
                }
                if (candidate.isBlank() || isParagraphBreak(candidate)) break
                quoted.add(candidate.trim())
                i++
            }
            out.add(MdBlock.Quote(annotate(quoted.joinToString("\n"), codeBg, link)))
            continue
        }

        if (BULLET.matchEntire(line) != null || NUMBERED.matchEntire(line) != null) {
            i = parseList(lines, i, out, codeBg, link)
            continue
        }

        val paragraph = ArrayList<String>()
        while (i < lines.size) {
            val candidate = lines[i]
            if (isParagraphBreak(candidate)) break
            paragraph.add(candidate.trim())
            i++
        }
        if (paragraph.isNotEmpty()) {
            out.add(MdBlock.Paragraph(annotate(paragraph.joinToString("\n"), codeBg, link)))
        }
    }

    return out
}

private data class RawListItem(
    val level: Int,
    val marker: String?,
    val number: Int?,
    val body: StringBuilder,
)

private fun parseList(
    lines: List<String>,
    start: Int,
    out: MutableList<MdBlock>,
    codeBg: Color,
    link: Color,
): Int {
    val items = ArrayList<RawListItem>()
    var i = start

    while (i < lines.size) {
        val line = lines[i]

        if (line.isBlank()) {
            val next = lines.getOrNull(i + 1)
            if (next != null && (BULLET.matchEntire(next) != null || NUMBERED.matchEntire(next) != null)) {
                i++
                continue
            }
            break
        }

        val bullet = BULLET.matchEntire(line)
        val numbered = NUMBERED.matchEntire(line)
        if (bullet != null) {
            val level = levelFor(indentWidth(bullet.groupValues[1]))
            val body = bullet.groupValues[3].trim()
            items.add(RawListItem(level, bullet.groupValues[2], null, StringBuilder(body)))
            i++
            continue
        }
        if (numbered != null) {
            val level = levelFor(indentWidth(numbered.groupValues[1]))
            val body = numbered.groupValues[3].trim()
            val number = numbered.groupValues[2].toIntOrNull() ?: 1
            items.add(RawListItem(level, null, number, StringBuilder(body)))
            i++
            continue
        }

        val last = items.lastOrNull()
        if (last != null && !isBlockStart(line)) {
            last.body.append('\n').append(line.trim())
            i++
            continue
        }
        break
    }

    val counters = HashMap<Int, Int>()
    items.forEach { item ->
        counters.keys.removeAll { it > item.level }
        val body = annotate(item.body.toString(), codeBg, link)
        val number = item.number
        if (number != null) {
            val index = (counters[item.level] ?: (number - 1)) + 1
            counters[item.level] = index
            out.add(MdBlock.Numbered(item.level, index, body))
        } else {
            counters.keys.removeAll { it >= item.level }
            out.add(MdBlock.Bullet(item.level, item.marker ?: "•", body))
        }
    }
    return i
}

private fun annotate(text: String, codeBg: Color, link: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in INLINE.findAll(text)) {
        if (match.range.first < cursor) continue
        if (match.range.first > cursor) append(text, cursor, match.range.first)

        val groups = match.groupValues
        val start = length
        when {
            groups[1].isNotEmpty() -> {
                append(groups[1])
                addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg),
                    start,
                    length,
                )
            }
            groups[2].isNotEmpty() -> {
                val url = groups[3]
                if (isSafeLinkDestination(url)) {
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                style = SpanStyle(
                                    color = link,
                                    textDecoration = TextDecoration.Underline,
                                )
                            )
                        )
                    ) {
                        append(groups[2])
                    }
                } else {
                    append(groups[2])
                }
            }
            groups[4].isNotEmpty() -> {
                append(groups[4])
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                    ),
                    start,
                    length,
                )
            }
            groups[5].isNotEmpty() -> {
                append(groups[5])
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                    ),
                    start,
                    length,
                )
            }
            groups[6].isNotEmpty() -> {
                append(groups[6])
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
            }
            groups[7].isNotEmpty() -> {
                append(groups[7])
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
            }
            groups[8].isNotEmpty() -> {
                append(groups[8])
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
            }
            groups[9].isNotEmpty() -> {
                append(groups[9])
                addStyle(SpanStyle(background = link.copy(alpha = 0.18f)), start, length)
            }
            groups[10].isNotEmpty() -> {
                append(groups[10])
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
            }
            groups[11].isNotEmpty() -> {
                append(groups[11])
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
            }
            groups[12].isNotEmpty() -> {
                val url = groups[12]
                if (isSafeLinkDestination(url)) {
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                style = SpanStyle(
                                    color = link,
                                    textDecoration = TextDecoration.Underline,
                                )
                            )
                        )
                    ) {
                        append(url)
                    }
                } else {
                    append(url)
                }
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text, cursor, text.length)
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val codeBg = scheme.surfaceContainerHigh
    val link = scheme.tertiary
    val blocks = remember(text, codeBg, link) { parseMarkdown(text, codeBg, link) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    color = scheme.onSurface,
                    fontSize = when (block.level) {
                        1 -> 24.sp
                        2 -> 21.sp
                        3 -> 18.sp
                        4 -> 17.sp
                        else -> 16.sp
                    },
                    lineHeight = when (block.level) {
                        1 -> 30.sp
                        2 -> 27.sp
                        3 -> 24.sp
                        else -> 22.sp
                    },
                    fontWeight = FontWeight.Bold,
                )

                is MdBlock.Paragraph -> Text(
                    text = block.text,
                    color = scheme.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )

                is MdBlock.Bullet -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (block.level * 14).dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = bulletMarker(block.level, block.marker),
                        color = scheme.primary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        text = block.text,
                        color = scheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                is MdBlock.Numbered -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (block.level * 14).dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "${block.index}.",
                        color = scheme.primary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = block.text,
                        color = scheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                is MdBlock.Quote -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = scheme.tertiary,
                                size = Size(3.dp.toPx(), size.height),
                            )
                        }
                        .padding(start = 13.dp, top = 2.dp, bottom = 2.dp),
                ) {
                    Text(
                        text = block.text,
                        color = scheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }

                is MdBlock.Code -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(codeBg),
                ) {
                    block.language?.let { language ->
                        Text(
                            text = language.uppercase(Locale.US),
                            color = scheme.tertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 14.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = block.code,
                            color = scheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }

                MdBlock.Rule -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.outlineVariant),
                )
            }
        }
    }
}

private fun bulletMarker(level: Int, original: String): String = when {
    original == "•" -> "•"
    level % 3 == 0 -> "•"
    level % 3 == 1 -> "◦"
    else -> "▪"
}
