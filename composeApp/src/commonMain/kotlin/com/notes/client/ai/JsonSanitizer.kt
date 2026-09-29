package com.notes.client.ai

import com.notes.common.models.NoteMetadataFill
import kotlinx.serialization.json.Json

object JsonSanitizer {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Sanitizes raw LLM output, strips markdown formatting, and decodes NoteMetadataFill.
     * Guarantees fallback if response is malformed or contains surrounding commentary.
     */
    fun parseMetadataResponse(
        rawResponse: String,
        existingTags: List<String> = emptyList()
    ): NoteMetadataFill {
        val trimmed = rawResponse.trim()

        // 1. Attempt extraction of JSON block from markdown fences or bare brackets
        val jsonCandidate = extractJsonString(trimmed)

        if (jsonCandidate != null) {
            try {
                val parsed = json.decodeFromString<NoteMetadataFill>(jsonCandidate)
                return sanitizeParsedFill(parsed, existingTags)
            } catch (_: Exception) {
                // Fall back to heuristic extraction below
            }
        }

        // 2. Heuristic fallback when LLM fails to output valid JSON
        return fallbackExtract(trimmed, existingTags)
    }

    private fun extractJsonString(input: String): String? {
        val firstBrace = input.indexOf('{')
        val lastBrace = input.lastIndexOf('}')

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return input.substring(firstBrace, lastBrace + 1)
        }
        return null
    }

    private fun sanitizeParsedFill(fill: NoteMetadataFill, existingTags: List<String>): NoteMetadataFill {
        val normalizedExisting = existingTags.map { normalizeTag(it) }.toSet()
        val cleanedTags = fill.suggestedTags
            .map { normalizeTag(it) }
            .filter { it.isNotBlank() && !normalizedExisting.contains(it) }
            .distinct()

        val cleanedTitle = fill.suggestedTitle?.trim()?.takeIf { it.isNotBlank() }
        val cleanedSummary = fill.summary?.trim()?.takeIf { it.isNotBlank() }

        return fill.copy(
            suggestedTitle = cleanedTitle,
            suggestedTags = cleanedTags,
            summary = cleanedSummary,
            suggestedWikilinks = fill.suggestedWikilinks.map { it.trim().removePrefix("[[").removeSuffix("]]").trim() }.filter { it.isNotBlank() }.distinct()
        )
    }

    fun normalizeTag(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9_-]".toRegex(), "")
    }

    private fun fallbackExtract(text: String, existingTags: List<String>): NoteMetadataFill {
        val normalizedExisting = existingTags.map { normalizeTag(it) }.toSet()
        val tagRegex = Regex("""#([a-zA-Z0-9_-]+)""")
        val foundTags = tagRegex.findAll(text)
            .map { normalizeTag(it.groupValues[1]) }
            .filter { it.isNotBlank() && !normalizedExisting.contains(it) }
            .distinct()
            .toList()

        return NoteMetadataFill(
            suggestedTags = foundTags,
            summary = text.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.take(200),
            detectedLanguage = "en"
        )
    }
}
