package com.notes.client.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure functional text transformation engine for Markdown editing actions.
 * Operates on [TextFieldValue] while preserving cursor stability and selection bounds.
 */
object MarkdownFormatter {

    /**
     * Wraps current selection in [prefix] and [suffix].
     * If selection is empty, inserts both delimiters and places cursor between them.
     */
    fun wrapSelection(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = value.text
        val selStart = minOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
        val selEnd = maxOf(value.selection.start, value.selection.end).coerceIn(0, text.length)

        return if (selStart == selEnd) {
            val newText = text.substring(0, selStart) + prefix + suffix + text.substring(selEnd)
            val newCursor = selStart + prefix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor, newCursor)
            )
        } else {
            val selectedText = text.substring(selStart, selEnd)
            val newText = text.substring(0, selStart) + prefix + selectedText + suffix + text.substring(selEnd)
            val newEnd = selEnd + prefix.length + suffix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(selStart, newEnd)
            )
        }
    }

    /**
     * Toggles a prefix (e.g. heading, quote, list bullet, or checklist) at the beginning
     * of the line where the cursor/selection currently resides.
     */
    fun toggleLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
        val text = value.text
        if (text.isEmpty()) {
            return TextFieldValue(prefix, TextRange(prefix.length))
        }

        val selStart = minOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
        val lastNewline = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
        val lineStart = if (lastNewline == -1 || selStart == 0) 0 else lastNewline + 1
        val nextNewline = text.indexOf('\n', selStart)
        val lineEnd = if (nextNewline == -1) text.length else nextNewline
        val currentLine = text.substring(lineStart, lineEnd)

        return if (currentLine.startsWith(prefix)) {
            // Remove prefix
            val newLine = currentLine.substring(prefix.length)
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            val newCursor = (selStart - prefix.length).coerceAtLeast(lineStart)
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        } else {
            // Prepend prefix
            val newLine = prefix + currentLine
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            val newCursor = selStart + prefix.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        }
    }

    // Typography
    fun applyBold(value: TextFieldValue): TextFieldValue = wrapSelection(value, "**", "**")

    fun applyItalic(value: TextFieldValue): TextFieldValue = wrapSelection(value, "*", "*")

    fun applyStrikethrough(value: TextFieldValue): TextFieldValue = wrapSelection(value, "~~", "~~")

    fun applyInlineCode(value: TextFieldValue): TextFieldValue = wrapSelection(value, "`", "`")

    // Headings
    fun applyHeading1(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "# ")

    fun applyHeading2(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "## ")

    fun applyHeading3(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "### ")

    // Structure & Blocks
    fun applyBlockquote(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "> ")

    fun applyCodeBlock(value: TextFieldValue, language: String = ""): TextFieldValue {
        val selStart = minOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
        val selEnd = maxOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
        val text = value.text

        return if (selStart == selEnd) {
            val prefix = "```$language\n"
            val suffix = "\n```\n"
            val newText = text.substring(0, selStart) + prefix + suffix + text.substring(selEnd)
            val newCursor = selStart + prefix.length
            TextFieldValue(newText, TextRange(newCursor))
        } else {
            val selected = text.substring(selStart, selEnd)
            val prefix = "```$language\n"
            val suffix = "\n```\n"
            val newText = text.substring(0, selStart) + prefix + selected + suffix + text.substring(selEnd)
            val newEnd = selEnd + prefix.length + suffix.length
            TextFieldValue(newText, TextRange(selStart, newEnd))
        }
    }

    fun applyTable(value: TextFieldValue): TextFieldValue {
        val tableTmpl = "\n| Column 1 | Column 2 |\n| :--- | :--- |\n| Data 1 | Data 2 |\n"
        return wrapSelection(value, "", tableTmpl)
    }

    fun applyMath(value: TextFieldValue): TextFieldValue = wrapSelection(value, "$", "$")

    // Lists
    fun applyBulletList(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "- ")

    fun applyTaskList(value: TextFieldValue): TextFieldValue = toggleLinePrefix(value, "- [ ] ")

    // Links & References
    fun applyWikilink(value: TextFieldValue): TextFieldValue = wrapSelection(value, "[[", "]]")

    fun applyWebLink(value: TextFieldValue): TextFieldValue = wrapSelection(value, "[", "](https://)")

    fun applyTag(value: TextFieldValue): TextFieldValue = wrapSelection(value, "#", "")
}
