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
    val action: (TextFieldValue) -> TextFieldValue = { it }
)

data class UserToolbarConfig(
    val activeButtons: List<String> = ToolbarRegistry.DEFAULT_MOBILE_BUTTON_IDS,
    val disabledButtons: List<String> = emptyList()
) {
    fun moveButton(fromIndex: Int, toIndex: Int): UserToolbarConfig {
        if (fromIndex !in activeButtons.indices || toIndex !in activeButtons.indices || fromIndex == toIndex) return this
        val list = activeButtons.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        return copy(activeButtons = list)
    }

    fun removeButton(buttonId: String): UserToolbarConfig {
        if (buttonId !in activeButtons) return this
        val newActive = activeButtons.filter { it != buttonId }
        val newDisabled = if (buttonId !in disabledButtons) disabledButtons + buttonId else disabledButtons
        return copy(activeButtons = newActive, disabledButtons = newDisabled)
    }

    fun addButton(buttonId: String): UserToolbarConfig {
        if (buttonId in activeButtons) return this
        val newActive = activeButtons + buttonId
        val newDisabled = disabledButtons.filter { it != buttonId }
        return copy(activeButtons = newActive, disabledButtons = newDisabled)
    }

    fun resetDefaults(): UserToolbarConfig = UserToolbarConfig(
        activeButtons = ToolbarRegistry.DEFAULT_MOBILE_BUTTON_IDS,
        disabledButtons = emptyList()
    )
}

object ToolbarRegistry {
    val ALL_ACTIONS: Map<String, ToolbarActionItem> = listOf(
        // Mobile 13 Core Benchmark Tools
        ToolbarActionItem("quick_commands", "?", "Quick Commands / Palette", fontWeight = FontWeight.Bold),
        ToolbarActionItem("undo", "↶", "Undo", fontSize = 16.sp, fontWeight = FontWeight.Bold),
        ToolbarActionItem("redo", "↷", "Redo", fontSize = 16.sp, fontWeight = FontWeight.Bold),
        ToolbarActionItem("tasks", "[ ]", "Task Checklist", icon = Icons.Default.CheckBox, action = { MarkdownFormatter.applyTaskList(it) }),
        ToolbarActionItem("wikilink", "[[ ]]", "Internal Wikilink", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyWikilink(it) }),
        ToolbarActionItem("tag", "#", "Tag Inserter", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyTag(it) }),
        ToolbarActionItem("attachment", "📎", "Attachment / Media", fontSize = 15.sp, action = { MarkdownFormatter.applyAttachment(it) }),
        ToolbarActionItem("heading_menu", "H", "Heading Menu", fontWeight = FontWeight.Bold),
        ToolbarActionItem("bold", "B", "Bold (Ctrl+B)", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyBold(it) }),
        ToolbarActionItem("italic", "I", "Italic (Ctrl+I)", fontStyle = FontStyle.Italic, action = { MarkdownFormatter.applyItalic(it) }),
        ToolbarActionItem("strikethrough", "S", "Strikethrough", textDecoration = TextDecoration.LineThrough, action = { MarkdownFormatter.applyStrikethrough(it) }),
        ToolbarActionItem("link", "🔗", "Web Link", icon = Icons.Default.Link, action = { MarkdownFormatter.applyWebLink(it) }),
        ToolbarActionItem("codeblock", "</>", "Code Block", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyCodeBlock(it) }),

        // Extended & Desktop Formatting Tools
        ToolbarActionItem("indent", ">≡", "Indent Line", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyIndent(it) }),
        ToolbarActionItem("outdent", "<≡", "Outdent Line", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyOutdent(it) }),
        ToolbarActionItem("code", "<>", "Inline Code", fontFamily = FontFamily.Monospace, action = { MarkdownFormatter.applyInlineCode(it) }),
        ToolbarActionItem("h1", "H1", "Heading 1", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyHeading1(it) }),
        ToolbarActionItem("h2", "H2", "Heading 2", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyHeading2(it) }),
        ToolbarActionItem("h3", "H3", "Heading 3", fontWeight = FontWeight.SemiBold, action = { MarkdownFormatter.applyHeading3(it) }),
        ToolbarActionItem("quote", "\"", "Blockquote", icon = Icons.Default.FormatQuote, action = { MarkdownFormatter.applyBlockquote(it) }),
        ToolbarActionItem("table", "Table", "Markdown Table", icon = Icons.Default.TableChart, action = { MarkdownFormatter.applyTable(it) }),
        ToolbarActionItem("math", "Σ", "Math Formula", fontSize = 15.sp, fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyMath(it) }),
        ToolbarActionItem("bullet", "•—", "Bullet List", fontWeight = FontWeight.Bold, action = { MarkdownFormatter.applyBulletList(it) })
    ).associateBy { it.id }

    /**
     * Default mobile 13-action sequence matching Obsidian Android benchmark
     */
    val DEFAULT_MOBILE_BUTTON_IDS: List<String> = listOf(
        "quick_commands", "undo", "redo", "tasks", "wikilink", "tag", "attachment",
        "heading_menu", "bold", "italic", "strikethrough", "link", "codeblock"
    )

    val DEFAULT_BUTTON_IDS: List<String> = listOf(
        "quick_commands", "undo", "redo", "tasks", "wikilink", "tag", "attachment", "heading_menu",
        "bold", "italic", "strikethrough", "code",
        "h1", "h2", "h3",
        "quote", "codeblock", "table", "math",
        "bullet", "link"
    )
}
