package com.notes.client.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * Engine 2: RichText WYSIWYG Renderer.
 * Live visual document rendering with styled callout cards, rich typography,
 * interactive checklists, and modern Obsidian/Material 3 surface treatments (ADR Q5).
 */
class RichTextMarkdownEngine : MarkdownEngine {
    override val id: String = "richtext-wysiwyg"
    override val displayName: String = "Engine 2 (WYSIWYG)"
    override val description: String = "Live rich-text WYSIWYG rendering with callouts and styled spans"

    @Composable
    override fun Render(
        content: String,
        modifier: Modifier,
        onLinkClick: (String) -> Unit
    ) {
        val lines = remember(content) { content.lines() }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(lines) { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("# ") -> {
                        Text(
                            text = line.removePrefix("# "),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    line.startsWith("## ") -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Text(
                                text = line.removePrefix("## "),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    line.startsWith("### ") -> {
                        Text(
                            text = line.removePrefix("### "),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    line.startsWith("> [!") -> {
                        // Callout block (e.g. > [!NOTE] text or > [!TIP] text)
                        val calloutType = line.substringAfter("[!").substringBefore("]")
                        val calloutText = line.substringAfter("]").trim()
                        CalloutCard(type = calloutType, text = calloutText)
                    }
                    line.startsWith("> ") -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = line.removePrefix("> "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                        var checked by remember(line) { mutableStateOf(true) }
                        InteractiveCheckRow(
                            text = line.substring(6).trim(),
                            isChecked = checked,
                            onToggle = { checked = !checked }
                        )
                    }
                    line.startsWith("- [ ] ") -> {
                        var checked by remember(line) { mutableStateOf(false) }
                        InteractiveCheckRow(
                            text = line.substring(6).trim(),
                            isChecked = checked,
                            onToggle = { checked = !checked }
                        )
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 8.dp)
                                    .size(6.dp),
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                            Text(
                                text = line.substring(2).trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    line.isNotBlank() -> {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CalloutCard(type: String, text: String) {
        val (accentColor, title) = when (type.uppercase()) {
            "NOTE" -> MaterialTheme.colorScheme.primary to "Note"
            "TIP" -> MaterialTheme.colorScheme.secondary to "Tip"
            "IMPORTANT" -> MaterialTheme.colorScheme.tertiary to "Important"
            "WARNING", "CAUTION" -> MaterialTheme.colorScheme.error to "Warning"
            else -> MaterialTheme.colorScheme.primary to type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💡 $title",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    @Composable
    private fun InteractiveCheckRow(
        text: String,
        isChecked: Boolean,
        onToggle: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggle() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
