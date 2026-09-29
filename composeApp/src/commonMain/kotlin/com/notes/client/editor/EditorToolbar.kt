package com.notes.client.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Multiplatform Markdown Editor Formatting Toolbar.
 * Provides high-speed formatting actions grounded in Obsidian and Samsung Notes UX benchmarks.
 */
@Composable
fun EditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Typography Group
            ToolbarButton(
                label = "B",
                title = "Bold (Ctrl+B)",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyBold(value)) }
            )
            ToolbarButton(
                label = "I",
                title = "Italic (Ctrl+I)",
                fontStyle = FontStyle.Italic,
                onClick = { onValueChange(MarkdownFormatter.applyItalic(value)) }
            )
            ToolbarButton(
                label = "S",
                title = "Strikethrough",
                textDecoration = TextDecoration.LineThrough,
                onClick = { onValueChange(MarkdownFormatter.applyStrikethrough(value)) }
            )
            ToolbarButton(
                label = "<>",
                title = "Inline Code",
                fontFamily = FontFamily.Monospace,
                onClick = { onValueChange(MarkdownFormatter.applyInlineCode(value)) }
            )

            ToolbarVerticalDivider()

            // Headings Group
            ToolbarButton(
                label = "H1",
                title = "Heading 1",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyHeading1(value)) }
            )
            ToolbarButton(
                label = "H2",
                title = "Heading 2",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyHeading2(value)) }
            )
            ToolbarButton(
                label = "H3",
                title = "Heading 3",
                fontWeight = FontWeight.SemiBold,
                onClick = { onValueChange(MarkdownFormatter.applyHeading3(value)) }
            )

            ToolbarVerticalDivider()

            // Structure Group
            ToolbarButton(
                label = "\"",
                icon = Icons.Default.FormatQuote,
                title = "Blockquote",
                onClick = { onValueChange(MarkdownFormatter.applyBlockquote(value)) }
            )
            ToolbarButton(
                label = "</>",
                title = "Code Block",
                fontFamily = FontFamily.Monospace,
                onClick = { onValueChange(MarkdownFormatter.applyCodeBlock(value)) }
            )
            ToolbarButton(
                label = "Table",
                icon = Icons.Default.TableChart,
                title = "Markdown Table",
                onClick = { onValueChange(MarkdownFormatter.applyTable(value)) }
            )
            ToolbarButton(
                label = "Σ",
                title = "Math Formula",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyMath(value)) }
            )

            ToolbarVerticalDivider()

            // Lists Group
            ToolbarButton(
                label = "•—",
                title = "Bullet List",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyBulletList(value)) }
            )
            ToolbarButton(
                label = "Tasks",
                icon = Icons.Default.CheckBox,
                title = "Task List",
                onClick = { onValueChange(MarkdownFormatter.applyTaskList(value)) }
            )

            ToolbarVerticalDivider()

            // Links & References Group
            ToolbarButton(
                label = "[[ ]]",
                title = "Internal Wikilink",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyWikilink(value)) }
            )
            ToolbarButton(
                label = "Link",
                icon = Icons.Default.Link,
                title = "Web Link",
                onClick = { onValueChange(MarkdownFormatter.applyWebLink(value)) }
            )
            ToolbarButton(
                label = "#",
                title = "Tag",
                fontWeight = FontWeight.Bold,
                onClick = { onValueChange(MarkdownFormatter.applyTag(value)) }
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .height(36.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = fontFamily,
                textDecoration = textDecoration
            )
        }
    }
}

@Composable
private fun ToolbarVerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
