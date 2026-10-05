package com.notes.client.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mobile-optimized Markdown Editor Formatting Toolbar.
 * Grounded in Obsidian Android & Samsung Notes UX benchmarks.
 * Features 48dp height, 48dp accessible touch targets (WCAG 2.1 AA),
 * Obsidian Dark design tokens (#1F1F23, #333338, #A78BFA),
 * heading dropdown menu, and customization modal trigger.
 */
@Composable
fun EditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    activeButtonIds: List<String> = ToolbarRegistry.DEFAULT_MOBILE_BUTTON_IDS,
    onQuickCommands: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    onAttachment: (() -> Unit)? = null,
    onOpenCustomizer: (() -> Unit)? = null,
    isDark: Boolean = true
) {
    // Design Tokens (Obsidian Dark vs Light)
    val surfaceColor = if (isDark) Color(0xFF1F1F23) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) Color(0xFF333338) else MaterialTheme.colorScheme.outlineVariant
    val activeBgColor = if (isDark) Color(0xFF2E283E) else MaterialTheme.colorScheme.primaryContainer
    val activeTextColor = if (isDark) Color(0xFFA78BFA) else MaterialTheme.colorScheme.primary
    val iconColor = if (isDark) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.onSurfaceVariant

    var showHeadingMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = surfaceColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            activeButtonIds.forEach { buttonId ->
                val actionItem = ToolbarRegistry.ALL_ACTIONS[buttonId]
                if (actionItem != null) {
                    Box {
                        CompactToolbarButton(
                            label = actionItem.label,
                            title = actionItem.title,
                            icon = actionItem.icon,
                            fontWeight = actionItem.fontWeight,
                            fontStyle = actionItem.fontStyle,
                            fontFamily = actionItem.fontFamily,
                            textDecoration = actionItem.textDecoration,
                            fontSize = actionItem.fontSize,
                            iconColor = iconColor,
                            onClick = {
                                when (buttonId) {
                                    "quick_commands" -> onQuickCommands?.invoke()
                                    "undo" -> onUndo?.invoke()
                                    "redo" -> onRedo?.invoke()
                                    "attachment" -> {
                                        if (onAttachment != null) {
                                            onAttachment.invoke()
                                        } else {
                                            onValueChange(actionItem.action(value))
                                        }
                                    }
                                    "heading_menu" -> showHeadingMenu = true
                                    else -> onValueChange(actionItem.action(value))
                                }
                            }
                        )

                        // Heading Level Dropdown
                        if (buttonId == "heading_menu") {
                            DropdownMenu(
                                expanded = showHeadingMenu,
                                onDismissRequest = { showHeadingMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Заголовок 1 (H1)") },
                                    onClick = {
                                        onValueChange(MarkdownFormatter.applyHeadingLevel(value, 1))
                                        showHeadingMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Заголовок 2 (H2)") },
                                    onClick = {
                                        onValueChange(MarkdownFormatter.applyHeadingLevel(value, 2))
                                        showHeadingMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Заголовок 3 (H3)") },
                                    onClick = {
                                        onValueChange(MarkdownFormatter.applyHeadingLevel(value, 3))
                                        showHeadingMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Обычный текст (Paragraph)") },
                                    onClick = {
                                        onValueChange(MarkdownFormatter.applyHeadingLevel(value, 0))
                                        showHeadingMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Customization Trigger Button (🔧)
            if (onOpenCustomizer != null) {
                VerticalDivider(
                    color = borderColor,
                    modifier = Modifier
                        .height(28.dp)
                        .padding(horizontal = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenCustomizer)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Настройка инструментария",
                        modifier = Modifier.size(18.dp),
                        tint = activeTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToolbarButton(
    label: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    fontSize: TextUnit = 13.sp,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
        } else {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                color = iconColor
            )
        }
    }
}
