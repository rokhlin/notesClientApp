package com.notes.client.ai

import com.notes.common.models.AiMetadataRequest

object PromptBuilder {

    fun buildSystemInstruction(maxTags: Int = 5, existingTags: List<String> = emptyList()): String {
        val existingClause = if (existingTags.isNotEmpty()) {
            "Existing tags for this note are: [${existingTags.joinToString(", ")}]. Do NOT repeat these existing tags."
        } else {
            "There are currently no existing tags for this note."
        }

        return """
            You are an expert knowledge management AI assistant.
            Your task is to analyze note content and extract concise metadata to organize the user's workspace.
            
            $existingClause
            
            Requirements:
            1. Suggest between 2 and $maxTags new, semantically relevant tags.
            2. Each tag must be lowercase, alphanumeric or hyphen-separated (kebab-case, e.g. "kotlin-multiplatform", "security", "architecture"). Never include the '#' symbol.
            3. Provide a concise 1-2 sentence executive summary of the core concepts in the note.
            4. If the title is "Untitled", empty, or generic, suggest an improved, descriptive title.
            5. Identify 1 to 4 key concepts that would make good Obsidian-style internal [[Wikilinks]].
            
            Output Format:
            You must respond ONLY with a raw, valid JSON object strictly matching this schema:
            {
              "suggestedTitle": "Suggested note title or null",
              "suggestedTags": ["tag1", "tag2"],
              "summary": "1-2 sentence summary of note",
              "suggestedWikilinks": ["Concept 1", "Concept 2"],
              "detectedLanguage": "en"
            }
            Do not wrap your response in markdown code blocks. Output raw JSON only.
        """.trimIndent()
    }

    fun buildUserContent(request: AiMetadataRequest): String {
        return ContextTruncator.truncate(
            title = request.title.ifBlank { "Untitled Note" },
            content = request.content
        )
    }
}
