package com.notes.client

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.notes.client.editor.MarkdownFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownFormatterTest {

    @Test
    fun testApplyBoldEmptySelection() {
        val initial = TextFieldValue("Hello world", TextRange(5))
        val result = MarkdownFormatter.applyBold(initial)
        assertEquals("Hello**** world", result.text)
        assertEquals(7, result.selection.start)
        assertEquals(7, result.selection.end)
    }

    @Test
    fun testApplyBoldWithSelection() {
        val initial = TextFieldValue("Hello world", TextRange(0, 5))
        val result = MarkdownFormatter.applyBold(initial)
        assertEquals("**Hello** world", result.text)
        assertEquals(0, result.selection.start)
        assertEquals(9, result.selection.end)
    }

    @Test
    fun testApplyItalic() {
        val initial = TextFieldValue("Note text", TextRange(5, 9))
        val result = MarkdownFormatter.applyItalic(initial)
        assertEquals("Note *text*", result.text)
        assertEquals(5, result.selection.start)
        assertEquals(11, result.selection.end)
    }

    @Test
    fun testApplyStrikethroughAndInlineCode() {
        val strike = MarkdownFormatter.applyStrikethrough(TextFieldValue("delete me", TextRange(0, 9)))
        assertEquals("~~delete me~~", strike.text)

        val code = MarkdownFormatter.applyInlineCode(TextFieldValue("val x = 1", TextRange(0, 9)))
        assertEquals("`val x = 1`", code.text)
    }

    @Test
    fun testToggleHeadings() {
        val initial = TextFieldValue("Title line", TextRange(3))
        // Apply H1
        val h1 = MarkdownFormatter.applyHeading1(initial)
        assertEquals("# Title line", h1.text)

        // Toggle H1 off
        val toggledOff = MarkdownFormatter.applyHeading1(h1)
        assertEquals("Title line", toggledOff.text)

        // Apply H2
        val h2 = MarkdownFormatter.applyHeading2(initial)
        assertEquals("## Title line", h2.text)

        // Apply H3
        val h3 = MarkdownFormatter.applyHeading3(initial)
        assertEquals("### Title line", h3.text)
    }

    @Test
    fun testToggleBlockquote() {
        val initial = TextFieldValue("A profound quote", TextRange(5))
        val quoted = MarkdownFormatter.applyBlockquote(initial)
        assertEquals("> A profound quote", quoted.text)

        val unquoted = MarkdownFormatter.applyBlockquote(quoted)
        assertEquals("A profound quote", unquoted.text)
    }

    @Test
    fun testListsAndTasks() {
        val line1 = TextFieldValue("Buy milk", TextRange(0))
        val bullet = MarkdownFormatter.applyBulletList(line1)
        assertEquals("- Buy milk", bullet.text)

        val task = MarkdownFormatter.applyTaskList(line1)
        assertEquals("- [ ] Buy milk", task.text)

        // Toggle task off
        val toggledTask = MarkdownFormatter.applyTaskList(task)
        assertEquals("Buy milk", toggledTask.text)
    }

    @Test
    fun testWikilinkAndWebLink() {
        val noteRef = TextFieldValue("Architecture Blueprint", TextRange(0, 22))
        val wikilink = MarkdownFormatter.applyWikilink(noteRef)
        assertEquals("[[Architecture Blueprint]]", wikilink.text)

        val webRef = TextFieldValue("Google", TextRange(0, 6))
        val webLink = MarkdownFormatter.applyWebLink(webRef)
        assertEquals("[Google](https://)", webLink.text)
    }

    @Test
    fun testCodeBlockAndTable() {
        val codeSnippet = TextFieldValue("fun main() {}", TextRange(0, 13))
        val codeBlock = MarkdownFormatter.applyCodeBlock(codeSnippet, "kotlin")
        assertTrue(codeBlock.text.startsWith("```kotlin\nfun main() {}\n```"))

        val empty = TextFieldValue("", TextRange(0))
        val table = MarkdownFormatter.applyTable(empty)
        assertTrue(table.text.contains("| Column 1 | Column 2 |"))
    }

    @Test
    fun testMathFormatting() {
        val formula = TextFieldValue("E = mc^2", TextRange(0, 8))
        val math = MarkdownFormatter.applyMath(formula)
        assertEquals("\$E = mc^2\$", math.text)
    }
}
