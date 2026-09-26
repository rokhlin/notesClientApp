package com.notes.client.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Engine 1: AST CommonMark Renderer.
 * High-speed, lightweight CommonMark rendering optimized for standard Markdown purists
 * and raw syntax fidelity (ADR Q5).
 */
class AstMarkdownEngine : MarkdownEngine {
    override val id: String = "ast-renderer"
    override val displayName: String = "Engine 1 (AST)"
    override val description: String = "Fast CommonMark AST parser for raw syntax fidelity"

    @Composable
    override fun Render(
        content: String,
        modifier: Modifier,
        onLinkClick: (String) -> Unit
    ) {
        val parsedBlocks = remember(content) { parseMarkdown(content) }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(parsedBlocks) { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                            2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                            else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
                            Text(
                                text = block.text,
                                style = style,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (block.level <= 2) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    is MarkdownBlock.CodeBlock -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = block.code,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is MarkdownBlock.BlockQuote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    is MarkdownBlock.ChecklistItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = block.isChecked,
                                onCheckedChange = { /* Interactive state update */ },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (block.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    is MarkdownBlock.ListItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    is MarkdownBlock.Paragraph -> {
                        val segments = remember(block.text) { WikilinkParser.parseInlineSegments(block.text) }
                        if (segments.size == 1 && segments.first() is InlineSegment.Text) {
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                segments.forEach { segment ->
                                    when (segment) {
                                        is InlineSegment.Text -> {
                                            Text(
                                                text = segment.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        is InlineSegment.Link -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                modifier = Modifier.clickable { onLinkClick(segment.wikilink.targetTitle) }
                                            ) {
                                                Text(
                                                    text = "🔗 " + segment.wikilink.displayLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseMarkdown(content: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = content.lines()
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                continue
            }

            when {
                trimmed.startsWith("#### ") -> blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("#### ").trim(), 4))
                trimmed.startsWith("### ") -> blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("### ").trim(), 3))
                trimmed.startsWith("## ") -> blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("## ").trim(), 2))
                trimmed.startsWith("# ") -> blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("# ").trim(), 1))
                trimmed.startsWith("> ") -> blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix("> ").trim()))
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
                    blocks.add(MarkdownBlock.ChecklistItem(trimmed.substring(6).trim(), isChecked = true))
                trimmed.startsWith("- [ ] ") ->
                    blocks.add(MarkdownBlock.ChecklistItem(trimmed.substring(6).trim(), isChecked = false))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                    blocks.add(MarkdownBlock.ListItem(trimmed.substring(2).trim()))
                trimmed.isNotBlank() -> blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.CodeBlock(codeBuffer.toString().trimEnd()))
        }

        return blocks
    }
}

sealed interface MarkdownBlock {
    data class Heading(val text: String, val level: Int) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class ListItem(val text: String) : MarkdownBlock
    data class ChecklistItem(val text: String, val isChecked: Boolean) : MarkdownBlock
}
