package com.notes.client

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.notes.client.editor.MarkdownFormatter
import com.notes.client.editor.ToolbarRegistry
import com.notes.client.editor.UserToolbarConfig
import kotlin.test.*

class MobileEditorToolbarTest {

    @Test
    fun testDefaultMobile13CoreActionsSequence() {
        val mobileActions = ToolbarRegistry.DEFAULT_MOBILE_BUTTON_IDS
        assertEquals(13, mobileActions.size, "Must contain exactly 13 core mobile actions per Obsidian benchmark")
        assertEquals("quick_commands", mobileActions[0])
        assertEquals("undo", mobileActions[1])
        assertEquals("redo", mobileActions[2])
        assertEquals("tasks", mobileActions[3])
        assertEquals("wikilink", mobileActions[4])
        assertEquals("tag", mobileActions[5])
        assertEquals("attachment", mobileActions[6])
        assertEquals("heading_menu", mobileActions[7])
        assertEquals("bold", mobileActions[8])
        assertEquals("italic", mobileActions[9])
        assertEquals("strikethrough", mobileActions[10])
        assertEquals("link", mobileActions[11])
        assertEquals("codeblock", mobileActions[12])

        // Verify every action is registered in ALL_ACTIONS
        mobileActions.forEach { actionId ->
            val item = ToolbarRegistry.ALL_ACTIONS[actionId]
            assertNotNull(item, "Action $actionId must be registered in ToolbarRegistry.ALL_ACTIONS")
            assertTrue(item.title.isNotBlank())
            assertTrue(item.label.isNotBlank())
        }
    }

    @Test
    fun testUserToolbarConfigMutations() {
        val config = UserToolbarConfig()
        assertEquals(13, config.activeButtons.size)

        // Reorder
        val reordered = config.moveButton(fromIndex = 0, toIndex = 2)
        assertEquals("undo", reordered.activeButtons[0])
        assertEquals("redo", reordered.activeButtons[1])
        assertEquals("quick_commands", reordered.activeButtons[2])

        // Remove
        val removed = reordered.removeButton("quick_commands")
        assertFalse(removed.activeButtons.contains("quick_commands"))
        assertTrue(removed.disabledButtons.contains("quick_commands"))
        assertEquals(12, removed.activeButtons.size)

        // Add
        val added = removed.addButton("quick_commands")
        assertTrue(added.activeButtons.contains("quick_commands"))
        assertFalse(added.disabledButtons.contains("quick_commands"))
        assertEquals(13, added.activeButtons.size)

        // Reset
        val reset = added.resetDefaults()
        assertEquals(ToolbarRegistry.DEFAULT_MOBILE_BUTTON_IDS, reset.activeButtons)
        assertTrue(reset.disabledButtons.isEmpty())
    }

    @Test
    fun testMarkdownFormatterAttachment() {
        val emptyVal = TextFieldValue("Hello world", TextRange(5, 5))
        val resultEmpty = MarkdownFormatter.applyAttachment(emptyVal)
        assertEquals("Hello![[]] world", resultEmpty.text)
        assertEquals(8, resultEmpty.selection.start)

        val selectedVal = TextFieldValue("Photo.png", TextRange(0, 9))
        val resultSelected = MarkdownFormatter.applyAttachment(selectedVal)
        assertEquals("![[Photo.png]]", resultSelected.text)
    }

    @Test
    fun testMarkdownFormatterIndentAndOutdent() {
        val textVal = TextFieldValue("Line 1", TextRange(0, 0))
        val indented = MarkdownFormatter.applyIndent(textVal)
        assertEquals("  Line 1", indented.text)

        val outdented = MarkdownFormatter.applyOutdent(indented)
        assertEquals("Line 1", outdented.text)
    }

    @Test
    fun testMarkdownFormatterHeadingLevel() {
        val initial = TextFieldValue("Title text", TextRange(0, 0))

        // Level 1
        val h1 = MarkdownFormatter.applyHeadingLevel(initial, 1)
        assertEquals("# Title text", h1.text)

        // Switch to Level 2
        val h2 = MarkdownFormatter.applyHeadingLevel(h1, 2)
        assertEquals("## Title text", h2.text)

        // Switch to Level 3
        val h3 = MarkdownFormatter.applyHeadingLevel(h2, 3)
        assertEquals("### Title text", h3.text)

        // Clear heading (Level 0)
        val cleared = MarkdownFormatter.applyHeadingLevel(h3, 0)
        assertEquals("Title text", cleared.text)
    }
}
