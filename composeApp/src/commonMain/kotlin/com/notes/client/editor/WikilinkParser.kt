package com.notes.client.editor

import com.notes.common.models.Note

data class Wikilink(
    val targetTitle: String,
    val alias: String? = null,
    val raw: String
) {
    val displayLabel: String
        get() = alias?.ifBlank { targetTitle } ?: targetTitle
}

data class Backlink(
    val sourceNoteId: String,
    val sourceNoteTitle: String,
    val snippet: String
)

object WikilinkParser {
    // Matches [[Note Title]] or [[Note Title|Custom Alias]]
    private val WIKILINK_REGEX = Regex("""\[\[([^\|\]]+)(?:\|([^\]]+))?\]\]""")

    /**
     * Extracts all wikilinks from a markdown text body.
     */
    fun extractWikilinks(text: String): List<Wikilink> {
        val matches = WIKILINK_REGEX.findAll(text)
        return matches.map { match ->
            val target = match.groupValues[1].trim()
            val alias = match.groupValues.getOrNull(2)?.trim()?.ifBlank { null }
            Wikilink(targetTitle = target, alias = alias, raw = match.value)
        }.toList()
    }

    /**
     * Calculates all incoming backlinks for a target note from the entire vault collection.
     */
    fun findBacklinks(targetTitle: String, allNotes: List<Note>): List<Backlink> {
        if (targetTitle.isBlank()) return emptyList()

        val results = mutableListOf<Backlink>()
        for (note in allNotes) {
            if (note.title.equals(targetTitle, ignoreCase = true)) continue

            val links = extractWikilinks(note.content)
            val matchingLink = links.find { it.targetTitle.equals(targetTitle, ignoreCase = true) }
            if (matchingLink != null) {
                val snippetLine = note.content.lines().find { it.contains(matchingLink.raw) } ?: matchingLink.raw
                results.add(
                    Backlink(
                        sourceNoteId = note.id,
                        sourceNoteTitle = note.title,
                        snippet = snippetLine.trim()
                    )
                )
            }
        }
        return results
    }

    /**
     * Splits a text paragraph into plain text and wikilink segments for inline rendering.
     */
    fun parseInlineSegments(text: String): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        var lastIndex = 0

        for (match in WIKILINK_REGEX.findAll(text)) {
            if (match.range.first > lastIndex) {
                segments.add(InlineSegment.Text(text.substring(lastIndex, match.range.first)))
            }
            val target = match.groupValues[1].trim()
            val alias = match.groupValues.getOrNull(2)?.trim()?.ifBlank { null }
            segments.add(InlineSegment.Link(Wikilink(target, alias, match.value)))
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            segments.add(InlineSegment.Text(text.substring(lastIndex)))
        }

        return segments
    }
}

sealed interface InlineSegment {
    data class Text(val content: String) : InlineSegment
    data class Link(val wikilink: Wikilink) : InlineSegment
}
