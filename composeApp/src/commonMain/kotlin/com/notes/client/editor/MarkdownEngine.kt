package com.notes.client.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Strategy contract for pluggable Markdown engines (ADR Q5).
 * Decouples the workspace editor canvas from specific AST or RichText parsing libraries.
 */
interface MarkdownEngine {
    val id: String
    val displayName: String
    val description: String

    @Composable
    fun Render(
        content: String,
        modifier: Modifier,
        onLinkClick: (String) -> Unit
    )
}
