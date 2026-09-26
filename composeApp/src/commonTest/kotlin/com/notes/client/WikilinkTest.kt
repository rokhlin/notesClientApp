package com.notes.client

import com.notes.client.editor.InlineSegment
import com.notes.client.editor.WikilinkParser
import com.notes.common.models.Note
import com.notes.common.models.NoteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WikilinkTest {

    @Test
    fun testWikilinkExtractionStandardAndAliased() {
        val markdown = """
            Welcome to [[Architecture Blueprint]] and check the [[Canvas Wireframes|Canvas Engine]].
            Also refer to standard link [Docs](https://kotlinlang.org).
        """.trimIndent()

        val links = WikilinkParser.extractWikilinks(markdown)
        assertEquals(2, links.size)

        assertEquals("Architecture Blueprint", links[0].targetTitle)
        assertEquals(null, links[0].alias)
        assertEquals("Architecture Blueprint", links[0].displayLabel)

        assertEquals("Canvas Wireframes", links[1].targetTitle)
        assertEquals("Canvas Engine", links[1].alias)
        assertEquals("Canvas Engine", links[1].displayLabel)
    }

    @Test
    fun testBacklinksCalculationAcrossVault() {
        val notes = listOf(
            Note(
                id = "1",
                title = "Architecture Blueprint",
                content = "# Architecture\nSee [[Getting Started with KMP]] and [[Canvas Wireframes]].",
                type = NoteType.TEXT
            ),
            Note(
                id = "2",
                title = "Getting Started with KMP",
                content = "Built according to [[Architecture Blueprint]].",
                type = NoteType.TEXT
            ),
            Note(
                id = "3",
                title = "Canvas Wireframes",
                content = "Vector canvas integrated with [[Architecture Blueprint]].",
                type = NoteType.CANVAS
            )
        )

        val backlinksToArch = WikilinkParser.findBacklinks("Architecture Blueprint", notes)
        assertEquals(2, backlinksToArch.size)
        assertTrue(backlinksToArch.any { it.sourceNoteTitle == "Getting Started with KMP" })
        assertTrue(backlinksToArch.any { it.sourceNoteTitle == "Canvas Wireframes" })

        val backlinksToKmp = WikilinkParser.findBacklinks("Getting Started with KMP", notes)
        assertEquals(1, backlinksToKmp.size)
        assertEquals("Architecture Blueprint", backlinksToKmp.first().sourceNoteTitle)
    }

    @Test
    fun testInlineSegmentsParsing() {
        val paragraph = "Read [[Architecture Blueprint]] for details and [[Canvas Wireframes|Canvas]]."
        val segments = WikilinkParser.parseInlineSegments(paragraph)

        assertEquals(5, segments.size)
        assertTrue(segments[0] is InlineSegment.Text)
        assertEquals("Read ", (segments[0] as InlineSegment.Text).content)

        assertTrue(segments[1] is InlineSegment.Link)
        assertEquals("Architecture Blueprint", (segments[1] as InlineSegment.Link).wikilink.targetTitle)

        assertTrue(segments[2] is InlineSegment.Text)
        assertEquals(" for details and ", (segments[2] as InlineSegment.Text).content)

        assertTrue(segments[3] is InlineSegment.Link)
        assertEquals("Canvas Wireframes", (segments[3] as InlineSegment.Link).wikilink.targetTitle)
        assertEquals("Canvas", (segments[3] as InlineSegment.Link).wikilink.alias)
        assertEquals("Canvas", (segments[3] as InlineSegment.Link).wikilink.displayLabel)

        assertTrue(segments[4] is InlineSegment.Text)
        assertEquals(".", (segments[4] as InlineSegment.Text).content)
    }
}
