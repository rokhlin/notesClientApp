package com.notes.client

import com.notes.client.components.ObsidianSidebarTab
import com.notes.common.models.Note
import com.notes.common.models.NoteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObsidianScaffoldTest {

    private val sampleNotes = listOf(
        Note(
            id = "1",
            title = "Architecture Blueprint",
            content = "# Architecture\nObsidian UX without tabs.",
            type = NoteType.TEXT,
            tags = listOf("architecture", "sdm", "starred")
        ),
        Note(
            id = "2",
            title = "Getting Started with KMP",
            content = "Compose Multiplatform shares UI across targets.",
            type = NoteType.TEXT,
            tags = listOf("kmp", "compose")
        ),
        Note(
            id = "3",
            title = "Canvas Wireframes",
            content = "Continuous vertical roll canvas engine.",
            type = NoteType.CANVAS,
            tags = listOf("canvas", "skia", "starred")
        )
    )

    @Test
    fun testQuickSwitcherFilteringByTitle() {
        val query = "blueprint"
        val filtered = sampleNotes.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.content.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals(1, filtered.size)
        assertEquals("Architecture Blueprint", filtered.first().title)
    }

    @Test
    fun testQuickSwitcherFilteringByTag() {
        val query = "starred"
        val filtered = sampleNotes.filter {
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.title == "Architecture Blueprint" })
        assertTrue(filtered.any { it.title == "Canvas Wireframes" })
    }

    @Test
    fun testQuickSwitcherEmptyQueryReturnsAll() {
        val query = ""
        val filtered = if (query.isBlank()) sampleNotes else sampleNotes.filter { it.title.contains(query) }
        assertEquals(3, filtered.size)
    }

    @Test
    fun testSidebarTabsEnumeration() {
        val tabs = ObsidianSidebarTab.entries
        assertEquals(4, tabs.size)
        assertTrue(tabs.contains(ObsidianSidebarTab.FILES))
        assertTrue(tabs.contains(ObsidianSidebarTab.SEARCH))
        assertTrue(tabs.contains(ObsidianSidebarTab.TAGS))
        assertTrue(tabs.contains(ObsidianSidebarTab.BOOKMARKS))
    }

    @Test
    fun testWordCountCalculation() {
        val content = "Continuous vertical roll canvas engine with Catmull-Rom splines."
        val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }
        assertEquals(8, wordCount)
    }

    @Test
    fun testTagAggregationAndFrequency() {
        val tagsMap = sampleNotes.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()

        assertEquals(2, tagsMap["starred"])
        assertEquals(1, tagsMap["architecture"])
        assertEquals(1, tagsMap["kmp"])
        assertEquals(1, tagsMap["skia"])
    }
}
