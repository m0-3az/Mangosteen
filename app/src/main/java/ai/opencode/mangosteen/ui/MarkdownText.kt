package ai.opencode.mangosteen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.opencode.mangosteen.ui.theme.MangosteenColors

sealed interface MdBlock {
    data class Paragraph(val text: AnnotatedString) : MdBlock
    data class Heading(val level: Int, val text: AnnotatedString) : MdBlock
    data class Bullet(val level: Int, val text: AnnotatedString) : MdBlock
    data class Numbered(val level: Int, val index: Int, val text: AnnotatedString) : MdBlock
    data class Quote(val text: AnnotatedString) : MdBlock
    data class Code(val code: String) : MdBlock
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^([-*+•])\\s+(.*)$")
private val NUMBERED = Regex("^(\\d+[.)])\\s+(.*)$")
private val INLINE = Regex(
    "==(.+?)=="
        + "|\\*\\*(.+?)\\*\\*"
        + "|__(.+?)__"
        + "|~~(.+?)~~"
        + "|\\*([^*\\n]+?)\\*"
        + "|(?<![\\w_])_([^_\\n]+?)_(?![\\w_])"
        + "|`([^`]+)`"
        + "|\\[([^\\]]+)\\]\\((\\S+?)\\)"
)

private fun parseMarkdown(text: String): List<MdBlock> {
    val out = ArrayList<MdBlock>()
    val buffer = ArrayList<String>()

    fun infer(listIndex: Int) = listIndex

    fun flush() {
        if (buffer.isEmpty()) return
        val start = buffer[0]
        val trimmed = start.trimStart()
        when {
            HEADING.containsMatchIn(trimmed) -> {
                val m = HEADING.find(trimmed)!!
                val level = m.groupValues[1].length
                val rest = m.groupValues[2]
                val body = (listOf(rest) + buffer.drop(1)).joinToString("\n")
                out.add(
                    MdBlock.Heading(level, annotate(body))
                )
            }
            BULLET.containsMatchIn(trimmed) -> {
                var idx = 0
                var prevLevel = 0
                val bullets = ArrayList<Pair<Int, String>>()
                for (line in buffer) {
                    val lineBody = line.trimStart()
                    val m = BULLET.find(lineBody)
                    if (m == null) {
                        if (bullets.isEmpty()) continue
                        val last = bullets.removeAt(bullets.lastIndex)
                        bullets.add(last.first to (last.second + "\n" + lineBody))
                    } else {
                        // approximate indent as one level per 2 leading spaces
                        val indent = line.length - line.trimStart().length
                        val lvl = (indent / 2).coerceAtMost(4)
                        val body = m.groupValues[2]
                        bullets.add(lvl to body)
                        prevLevel = lvl
                        idx++
                    }
                }
                bullets.forEachIndexed { i, (lvl, body) ->
                    out.add(MdBlock.Bullet(lvl, annotate(body)))
                }
            }
            NUMBERED.containsMatchIn(trimmed) -> {
                var i = 1
                var anySeen = false
                buffer.forEach { line ->
                    val body = line.trimStart()
                    val m = NUMBERED.find(body)
                    if (m != null) {
                        anySeen = true
                        out.add(MdBlock.Numbered(0, i++, annotate(m.groupValues[2])))
                    } else if (anySeen) {
                        val last = out.removeAt(out.lastIndex) as MdBlock.Numbered
                        out.add(MdBlock.Numbered(last.level, last.index, annotate(last.text.toString() + "\n" + body)))
                    }
                }
            }
            trimmed.startsWith(">") -> {
                val body = buffer.joinToString("\n") { it.trimStart().removePrefix("> ").trimStart() }
                out.add(MdBlock.Quote(annotate(body)))
            }
            else -> out.add(MdBlock.Paragraph(annotate(buffer.joinToString("\n"))))
        }
        buffer.clear()
    }

    var code = StringBuffer()
    var inCode = false
    for (raw in text.lines()) {
        if (raw.trimStart().startsWith("```")) {
            if (inCode) {
                out.add(MdBlock.Code(code.toString().trimEnd('\n')))
                code = StringBuffer()
                inCode = false
                continue
            } else {
                flush()
                inCode = true
                continue
            }
        }
        if (inCode) {
            code.append(raw).append('\n')
        } else {
            if (raw.isBlank()) {
                flush()
            } else {
                buffer.add(raw)
            }
        }
    }
    if (inCode && code.isNotEmpty()) out.add(MdBlock.Code(code.toString().trimEnd('\n')))
    flush()
    return out
}

private fun annotate(text: String): AnnotatedString = buildAnnotatedString {
    text.split("\n").forEachIndexed { i, line ->
        if (i > 0) append('\n')
        var cursor = 0
        for (match in INLINE.findAll(line)) {
            if (match.range.first > cursor) append(line.substring(cursor, match.range.first))
            val g = match.groupValues
            val start = length
            when {
                g[1].isNotEmpty() -> {
                    append(g[1]); addStyle(SpanStyle(fontWeight = FontWeight.Medium), start, length)
                }
                g[2].isNotEmpty() -> {
                    append(g[2]); addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                }
                g[3].isNotEmpty() -> {
                    append(g[3]); addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                }
                g[4].isNotEmpty() -> {
                    append(g[4]); addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                }
                g[5].isNotEmpty() -> {
                    append(g[5]); addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                }
                g[6].isNotEmpty() -> {
                    append(g[6]); addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                }
                g[7].isNotEmpty() -> {
                    append(g[7]); addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MangosteenColors.CodeBg), start, length)
                }
                g[8].isNotEmpty() -> {
                    append(g[8]); addStyle(SpanStyle(color = MangosteenColors.Link, textDecoration = TextDecoration.Underline), start, length)
                }
            }
            cursor = match.range.last + 1
        }
        if (cursor < line.length) append(line.substring(cursor))
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MangosteenColors.TextPrimary,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    color = color,
                    fontSize = when (block.level) {
                        1 -> 24.sp; 2 -> 21.sp; 3 -> 18.sp; 4 -> 17.sp; else -> 16.sp
                    },
                    lineHeight = when (block.level) {
                        1 -> 30.sp; 2 -> 27.sp; 3 -> 24.sp; else -> 21.sp
                    },
                    fontWeight = FontWeight.Bold,
                )
                is MdBlock.Paragraph -> Text(
                    text = block.text,
                    color = color,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                is MdBlock.Bullet -> Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "•  ",
                        color = MangosteenColors.TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                    Text(
                        text = block.text,
                        color = color,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Spacer(Modifier.width(0.dp))
                }
                is MdBlock.Numbered -> Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${block.index}. ",
                        color = MangosteenColors.TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = block.text,
                        color = color,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }
                is MdBlock.Quote -> Text(
                    text = block.text,
                    color = MangosteenColors.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
                is MdBlock.Code -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MangosteenColors.CodeBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = block.code,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}
