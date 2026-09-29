package com.notes.client.ai

object ContextTruncator {
    private const val MAX_CONTEXT_LENGTH = 32_000

    /**
     * Bounding algorithm: Ensures the prompt context does not exceed [maxChars].
     * If content exceeds the threshold, retains the title, heading lines, and the
     * leading content up to [maxChars], appending a truncation indicator.
     */
    fun truncate(title: String, content: String, maxChars: Int = MAX_CONTEXT_LENGTH): String {
        val headerContext = "Title: $title\n\n"
        val availableForContent = maxChars - headerContext.length

        if (availableForContent <= 0) {
            return headerContext.take(maxChars)
        }

        if (content.length <= availableForContent) {
            return headerContext + content
        }

        // Extract key structural lines (markdown headings #) from across the doc
        val lines = content.lines()
        val structuralHeadings = lines.filter { it.trimStart().startsWith("#") }.joinToString("\n")
        val structuralPrefix = if (structuralHeadings.isNotBlank()) "Key Headings Outline:\n$structuralHeadings\n\nContent Excerpt:\n" else ""

        val remainingBudget = availableForContent - structuralPrefix.length - 60
        val bodyExcerpt = if (remainingBudget > 200) {
            content.take(remainingBudget)
        } else {
            content.take(availableForContent - 60)
        }

        return "$headerContext$structuralPrefix$bodyExcerpt\n\n... [Content truncated for AI context limits]"
    }
}
