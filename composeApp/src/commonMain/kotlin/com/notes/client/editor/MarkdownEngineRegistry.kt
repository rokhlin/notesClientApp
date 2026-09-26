package com.notes.client.editor

/**
 * Registry managing all pluggable markdown rendering engines (ADR Q5).
 */
object MarkdownEngineRegistry {
    val astEngine = AstMarkdownEngine()
    val richTextEngine = RichTextMarkdownEngine()

    val availableEngines: List<MarkdownEngine> = listOf(
        astEngine,
        richTextEngine
    )

    val defaultEngine: MarkdownEngine = astEngine

    fun getEngine(id: String): MarkdownEngine {
        return availableEngines.find { it.id == id } ?: defaultEngine
    }
}
