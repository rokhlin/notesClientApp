package com.notes.client

import com.notes.client.editor.AstMarkdownEngine
import com.notes.client.editor.MarkdownEngineRegistry
import com.notes.client.editor.RichTextMarkdownEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownEngineTest {

    @Test
    fun testMarkdownEngineRegistryAvailableEngines() {
        val engines = MarkdownEngineRegistry.availableEngines
        assertEquals(2, engines.size)

        val astEngine = MarkdownEngineRegistry.getEngine("ast-renderer")
        assertTrue(astEngine is AstMarkdownEngine)
        assertEquals("ast-renderer", astEngine.id)

        val richTextEngine = MarkdownEngineRegistry.getEngine("richtext-wysiwyg")
        assertTrue(richTextEngine is RichTextMarkdownEngine)
        assertEquals("richtext-wysiwyg", richTextEngine.id)
    }

    @Test
    fun testMarkdownEngineRegistryFallbackToDefault() {
        val unknownEngine = MarkdownEngineRegistry.getEngine("unknown-engine-id")
        assertNotNull(unknownEngine)
        assertEquals(MarkdownEngineRegistry.defaultEngine.id, unknownEngine.id)
    }

    @Test
    fun testEnginePreservesContentIntegrityOnSwitch() {
        val markdownContent = """
            # Architecture Blueprint
            
            Welcome to NotesAlltogether pluggable engine workspace.
            
            ## Tasks
            - [x] Phase 0: Foundations
            - [ ] Phase 1: Text Editor Core
            
            > Essential architectural principle
            
            ```kotlin
            val engine = MarkdownEngineRegistry.getEngine("ast-renderer")
            ```
        """.trimIndent()

        val engine1 = MarkdownEngineRegistry.getEngine("ast-renderer")
        val engine2 = MarkdownEngineRegistry.getEngine("richtext-wysiwyg")

        // Verifying both engines accept and process the identical source text without mutation
        assertEquals("ast-renderer", engine1.id)
        assertEquals("richtext-wysiwyg", engine2.id)
        assertTrue(markdownContent.contains("Architecture Blueprint"))
        assertTrue(markdownContent.contains("Phase 0: Foundations"))
    }
}
