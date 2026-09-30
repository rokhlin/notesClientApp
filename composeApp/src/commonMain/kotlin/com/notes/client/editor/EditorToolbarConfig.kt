package com.notes.client.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class ToolbarActionItem(
    val id: String,
    val label: String,
    val title: String,
    val icon: ImageVector? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val textDecoration: TextDecoration? = null,
    val fontSize: TextUnit = 13.sp,
    val action: (TextFieldValue) -> TextFieldValue
)

object ToolbarRegistry {
    val ALL_ACTIONS: Map<String, ToolbarActionItem> = listOf(
        ToolbarActionItem("bold", "B", "Bold (Ctrl+B)", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyBold(it) }),
        ToolbarActionItem("italic", "I", "Italic (Ctrl+I)", fontStyle = FontStyle.Italic, action = { MarkdownFormatter.applyItalic(it) }),
        ToolbarActionItem("strikethrough", "S", "Strikethrough", textDecoration = TextDecoration.LineThrough, action = { MarkdownFormatter.applyStrikethrough(it) }),
        ToolbarActionItem("code", "<>", "Inline Code", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyInlineCode(it) }),
        ToolbarActionItem("h1", "H1", "Heading 1", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyHeading1(it) }),
        ToolbarActionItem("h2", "H2", "Heading 2", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyHeading2(it) }),
        ToolbarActionItem("h3", "H3", "Heading 3", fontWeight = FontWeight.SemiBold, action = { MarkdownFormatter.applyHeading3(it) }),
        ToolbarActionItem("quote", "\"", "Blockquote", icon = Icons.Default.FormatQuote, action = { MarkdownFormatter.applyBlockquote(it) }),
        ToolbarActionItem("codeblock", "</>", "Code Block", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyCodeBlock(it) }),
        ToolbarActionItem("table", "Table", "Markdown Table", icon = Icons.Default.TableChart, action = { MarkdownFormatter.applyTable(it) }),
        ToolbarActionItem("math", "Σ", "Math Formula", fontSize = 15.sp, fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyMath(it) }),
        ToolbarActionItem("bullet", "•—", "Bullet List", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyBulletList(it) }),
        ToolbarActionItem("tasks", "Tasks", "Task List", icon = Icons.Default.CheckBox, action = { MarkdownFormatter.applyTaskList(it) }),
        ToolbarActionItem("wikilink", "[[ ]]", "Internal Wikilink", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyWikilink(it) }),
        ToolbarActionItem("link", "Link", "Web Link", icon = Icons.Default.Link, action = { MarkdownFormatter.applyWebLink(it) }),
        ToolbarActionItem("tag", "#", "Tag", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyTag(it) })
    ).associateBy { it.id }

    val DEFAULT_BUTTON_IDS: List<String> = listOf(
        "bold", "italic", "strikethrough", "code",
        "h1", "h2", "h3",
        "quote", "codeblock", "table", "math",
        "bullet", "tasks",
        "wikilink", "link", "tag"
    )
}
