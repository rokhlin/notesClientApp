package com.notes.client.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Mobile Customization Modal Sheet ("Настройка инструментария").
 * Grounded in the Obsidian Android reference benchmark (obsidian_android_analysis.md).
 * Allows users to reorder actions, delete items, add commands from the catalog,
 * and access extended editor settings.
 */
@Composable
fun ToolbarCustomizationSheet(
    config: UserToolbarConfig,
    onConfigChange: (UserToolbarConfig) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Obsidian Dark vs Light Colors
    val surfaceColor = if (isDark) Color(0xFF1F1F23) else MaterialTheme.colorScheme.surface
    val elevatedColor = if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isDark) Color(0xFF333338) else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isDark) Color(0xFFF4F4F5) else MaterialTheme.colorScheme.onSurface
    val textMutedColor = if (isDark) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentPurple = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
    val dangerRed = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = surfaceColor,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = textColor
                            )
                        }
                        Text(
                            text = "Настройка инструментария",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = textColor
                        )
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Active Items List & Controls
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "АКТИВНЫЕ ИНСТРУМЕНТЫ (${config.activeButtons.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textMutedColor
                            )
                            TextButton(
                                onClick = { onConfigChange(config.resetDefaults()) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("По умолчанию", fontSize = 12.sp, color = accentPurple)
                            }
                        }
                    }

                    itemsIndexed(config.activeButtons) { index, buttonId ->
                        val item = ToolbarRegistry.ALL_ACTIONS[buttonId]
                        val label = item?.title ?: buttonId
                        val glyph = item?.label ?: "?"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(elevatedColor)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = glyph,
                                        fontWeight = FontWeight.Bold,
                                        color = accentPurple,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onConfigChange(config.moveButton(index, index - 1)) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = textMutedColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (index < config.activeButtons.size - 1) {
                                    IconButton(
                                        onClick = { onConfigChange(config.moveButton(index, index + 1)) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = textMutedColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onConfigChange(config.removeButton(buttonId)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Удалить",
                                        tint = dangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accentPurple
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(accentPurple)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Добавить команду...", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Extended Settings Catalog ("Другие настройки")
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ДРУГИЕ НАСТРОЙКИ РЕДАКТОРА",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textMutedColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = elevatedColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• Увеличить / Уменьшить отступ: >≡ / <≡", fontSize = 12.sp, color = textMutedColor)
                                Text("• Свернуть / Развернуть заголовок: ±", fontSize = 12.sp, color = textMutedColor)
                                Text("• Свернуть все заголовки: ↗↙", fontSize = 12.sp, color = textMutedColor)
                                Text("• Прямые уровни: H1, H2, H3", fontSize = 12.sp, color = textMutedColor)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Command Modal Dialog
    if (showAddDialog) {
        val availableToAdd = ToolbarRegistry.ALL_ACTIONS.keys.filter { it !in config.activeButtons }
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Добавить инструмент в ленту",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (availableToAdd.isEmpty()) {
                        Text("Все доступные инструменты уже добавлены!", color = textMutedColor, fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableToAdd.size) { i ->
                                val buttonId = availableToAdd[i]
                                val item = ToolbarRegistry.ALL_ACTIONS[buttonId]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onConfigChange(config.addButton(buttonId))
                                            showAddDialog = false
                                        }
                                        .background(elevatedColor)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = item?.label ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = accentPurple,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item?.title ?: buttonId,
                                        color = textColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showAddDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Отмена", color = accentPurple)
                    }
                }
            }
        }
    }
}
