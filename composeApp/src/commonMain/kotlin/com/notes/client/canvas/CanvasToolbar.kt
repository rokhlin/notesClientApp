package com.notes.client.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                icon = Icons.Default.Edit,
                label = "Pen",
                isSelected = currentBrush.toolType == ToolType.PEN,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.PEN, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = Icons.Default.Create,
                label = "Fountain",
                isSelected = currentBrush.toolType == ToolType.FOUNTAIN_PEN,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.FOUNTAIN_PEN, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = Icons.Default.ModeEdit,
                label = "Pencil",
                isSelected = currentBrush.toolType == ToolType.PENCIL,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.PENCIL, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = Icons.Default.Brush,
                label = "Brush",
                isSelected = currentBrush.toolType == ToolType.CALLIGRAPHY_BRUSH,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.CALLIGRAPHY_BRUSH, currentBrush.color, currentBrush.baseWidth)) }
            )
            ToolButton(
                icon = Icons.Default.Highlight,
                label = "Highlight",
                isSelected = currentBrush.toolType == ToolType.HIGHLIGHTER,
                onClick = { onBrushChange(BrushConfig.defaultFor(ToolType.HIGHLIGHTER, currentBrush.color, currentBrush.baseWidth)) }
            )

            // Shape Insertion Tool
            Box {
                ToolButton(
                    icon = Icons.Default.Category,
                    label = "Shapes",
                    isSelected = showShapesMenu,
                    onClick = { showShapesMenu = !showShapesMenu }
                )

                DropdownMenu(
                    expanded = showShapesMenu,
                    onDismissRequest = { showShapesMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Straight Line") },
                        leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.STRAIGHT_LINE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rectangle") },
                        leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.RECTANGLE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Circle") },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.CIRCLE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ellipse") },
                        leadingIcon = { Icon(Icons.Default.Egg, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.ELLIPSE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Triangle") },
                        leadingIcon = { Icon(Icons.Default.ChangeHistory, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showShapesMenu = false
                            onInsertShape?.invoke(RecognizedShapeType.TRIANGLE)
                        }
                    )
                }
            }

            ToolButton(
                icon = Icons.Default.AutoFixNormal,
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
        Text(
            label,
            fontSize = 8.sp,
            color = contentColor,
            maxLines = 1
        )
    }
}
