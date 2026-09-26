package com.notes.client.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.shapes.RecognizedShapeType
import com.notes.common.models.ToolType

@Composable
fun CanvasToolbar(
    currentBrush: BrushConfig,
    onBrushChange: (BrushConfig) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onInsertShape: ((RecognizedShapeType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showShapesMenu by remember { mutableStateOf(false) }

    val quickColors = listOf(
        Color(0xFF0F172A),
        Color(0xFF4F46E5),
        Color(0xFFE11D48),
        Color(0xFF059669),
        Color(0xFFD97706),
        Color(0xFF7C3AED)
    )

    Surface(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Tool Selection Group
            ToolButton(
                icon = "🖊️",
                label = "Pen",
                isSelected = currentBrush.toolType == ToolType.PEN,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.PEN, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = "✒️",
                label = "Fountain",
                isSelected = currentBrush.toolType == ToolType.FOUNTAIN_PEN,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.FOUNTAIN_PEN, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = "✏️",
                label = "Pencil",
                isSelected = currentBrush.toolType == ToolType.PENCIL,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.PENCIL, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = "🖌️",
                label = "Brush",
                isSelected = currentBrush.toolType == ToolType.CALLIGRAPHY_BRUSH,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.CALLIGRAPHY_BRUSH, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = "🖍️",
                label = "Highlight",
                isSelected = currentBrush.toolType == ToolType.HIGHLIGHTER,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.HIGHLIGHTER, currentBrush.color, currentBrush.baseWidth)) }
            )

            // Shape Insertion Tool
            Box {
                ToolButton(
                    icon = "🔷",
                    label = "Shapes",
                    isSelected = showShapesMenu,
                    onClick = { showShapesMenu = !showShapesMenu }
                )

                DropdownMenu(
                    expanded = showShapesMenu,
                    onDismissRequest = { showShapesMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📏 Straight Line") },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.STRAIGHT_LINE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("▭ Rectangle") },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.RECTANGLE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("◯ Circle") },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.CIRCLE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⬭ Ellipse") },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.ELLIPSE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("△ Triangle") },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.TRIANGLE)
                        }
                    )
                }
            }

            ToolButton(
                icon = "🧹",
                label = "Eraser",
                isSelected = currentBrush.toolType == ToolType.VECTOR_ERASER,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.VECTOR_ERASER, currentBrush.color, currentBrush.baseWidth)) }
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 2. Color Swatches (hidden when eraser is active)
            if (currentBrush.toolType != ToolType.VECTOR_ERASER) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickColors.forEach { color ->
                        val isSelected = currentBrush.color == color
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onBrushChange(currentBrush.copy(color = color))
                                }
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            // 3. Width Slider with live dot preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(currentBrush.baseWidth.coerceIn(2f, 20f).dp)
                            .clip(CircleShape)
                            .background(if (currentBrush.toolType == ToolType.VECTOR_ERASER) MaterialTheme.colorScheme.onSurface else currentBrush.color)
                    )
                }

                Slider(
                    value = currentBrush.baseWidth,
                    onValueChange = { onBrushChange(currentBrush.copy(baseWidth = it)) },
                    valueRange = 1f..24f,
                    modifier = Modifier.width(90.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 4. Undo / Redo / Clear Actions
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Text("↩️", fontSize = 16.sp)
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Text("↪️", fontSize = 16.sp)
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp)
            ) {
                Text("🗑️", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 18.sp)
        Text(
            label,
            fontSize = 8.sp,
            color = contentColor,
            maxLines = 1
        )
    }
}
