package com.notes.client.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.shapes.RecognizedShapeType
import com.notes.client.canvas.shapes.ShapeRecognizer
import com.notes.client.canvas.spline.CatmullRomConverter
import com.notes.client.canvas.spline.StrokePoint
import com.notes.client.util.currentTimeMillis
import com.notes.common.models.*
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

data class RenderableStroke(
    val stroke: InkStroke,
    val brush: BrushConfig,
    val cachedPath: Path = CatmullRomConverter.inkPointsToCubicPath(stroke.points, stroke.strokeWidth)
)

@Composable
fun SkiaHandwrittenCanvas(
    modifier: Modifier = Modifier,
    layers: List<CanvasLayer>,
    onLayersChange: (List<CanvasLayer>) -> Unit,
    currentBrush: BrushConfig,
    pendingShapeType: RecognizedShapeType? = null,
    onShapePlaced: ((CanvasShape) -> Unit)? = null,
    onCancelShapePlacement: (() -> Unit)? = null,
    autoSnap: Boolean = false,
    pageHeight: Float = 1200f
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Active in-progress stroke points
    val activePoints = remember { mutableStateListOf<StrokePoint>() }

    // Shape placement drag coordinates
    var shapeDragStart by remember { mutableStateOf<Offset?>(null) }
    var shapeDragCurrent by remember { mutableStateOf<Offset?>(null) }

    // Selected shape for Samsung Notes-style manipulation
    var selectedShapeId by remember { mutableStateOf<String?>(null) }

    // Quick palette for shape color editing
    val shapeColors = listOf(
        Color(0xFF0F172A),
        Color(0xFF4F46E5),
        Color(0xFFE11D48),
        Color(0xFF059669),
        Color(0xFFD97706),
        Color(0xFF7C3AED)
    )

    // Multi-touch pinch-to-zoom and pan state
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3.0f)
        offset += panChange
    }

    // Cached renderable strokes per layer
    val renderableLayers = remember(layers) {
        layers.map { layer ->
            layer.copy() to layer.strokes.map { stroke ->
                val brush = BrushConfig.defaultFor(
                    tool = stroke.tool,
                    color = parseColorHex(stroke.colorHex),
                    baseWidth = stroke.strokeWidth
                )
                RenderableStroke(stroke, brush)
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val canvasBg = if (isDark) Color(0xFF161822) else Color(0xFFFAF9F6)
    val dividerColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    val currentLayersState by rememberUpdatedState(layers)
    val currentOnLayersChange by rememberUpdatedState(onLayersChange)
    val currentBrushState by rememberUpdatedState(currentBrush)

    // Helper functions for updating canvas layers
    fun updateShapeInLayers(updatedShape: CanvasShape) {
        val updated = currentLayersState.map { layer ->
            layer.copy(
                shapes = layer.shapes.map { if (it.id == updatedShape.id) updatedShape else it }
            )
        }
        currentOnLayersChange(updated)
    }

    fun deleteShapeFromLayers(shapeId: String) {
        val updated = currentLayersState.map { layer ->
            layer.copy(
                shapes = layer.shapes.filterNot { it.id == shapeId }
            )
        }
        currentOnLayersChange(updated)
        selectedShapeId = null
    }

    fun updateTextBoxInLayers(updatedBox: CanvasTextBox) {
        val updated = currentLayersState.map { layer ->
            layer.copy(
                textBoxes = layer.textBoxes.map { if (it.id == updatedBox.id) updatedBox else it }
            )
        }
        currentOnLayersChange(updated)
    }

    fun deleteTextBoxFromLayers(boxId: String) {
        val updated = currentLayersState.map { layer ->
            layer.copy(
                textBoxes = layer.textBoxes.filterNot { it.id == boxId }
            )
        }
        currentOnLayersChange(updated)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            // Tap gestures: Long-press to select shapes, tap to deselect
            .pointerInput(layers, offset, scale) {
                detectTapGestures(
                    onTap = {
                        selectedShapeId = null
                    },
                    onLongPress = { touchOffset ->
                        val localPoint = (touchOffset - offset) / scale
                        // Hit-test shapes in topmost order
                        val hit = currentLayersState.flatMap { it.shapes }.reversed().firstOrNull { shape ->
                            val hw = kotlin.math.abs(shape.width) / 2f + 16f
                            val hh = kotlin.math.abs(shape.height) / 2f + 16f
                            localPoint.x in (shape.x - hw)..(shape.x + hw) &&
                                    localPoint.y in (shape.y - hh)..(shape.y + hh)
                        }
                        if (hit != null) {
                            selectedShapeId = hit.id
                        }
                    }
                )
            }
            // Drag gestures: drawing strokes or positioning shapes
            .pointerInput(currentBrush, scale, offset, pendingShapeType, autoSnap) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val localPoint = (startOffset - offset) / scale
                        if (pendingShapeType != null) {
                            shapeDragStart = localPoint
                            shapeDragCurrent = localPoint
                        } else {
                            activePoints.clear()
                            activePoints.add(
                                StrokePoint(
                                    x = localPoint.x,
                                    y = localPoint.y,
                                    pressure = 0.5f,
                                    timestamp = currentTimeMillis()
                                )
                            )
                        }
                    },
                    onDrag = { change, _ ->
                        val localPoint = (change.position - offset) / scale
                        if (pendingShapeType != null) {
                            shapeDragCurrent = localPoint
                        } else {
                            val pressure = change.pressure.coerceIn(0.1f, 1.0f)
                            activePoints.add(
                                StrokePoint(
                                    x = localPoint.x,
                                    y = localPoint.y,
                                    pressure = pressure,
                                    timestamp = currentTimeMillis()
                                )
                            )
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        if (pendingShapeType != null && shapeDragStart != null && shapeDragCurrent != null) {
                            val start = shapeDragStart!!
                            val curr = shapeDragCurrent!!
                            val cx: Float
                            val cy: Float
                            val w: Float
                            val h: Float

                            if (pendingShapeType == RecognizedShapeType.STRAIGHT_LINE) {
                                cx = (start.x + curr.x) / 2f
                                cy = (start.y + curr.y) / 2f
                                w = curr.x - start.x
                                h = curr.y - start.y
                            } else {
                                cx = start.x
                                cy = start.y
                                val rx = kotlin.math.max(16f, kotlin.math.abs(curr.x - cx))
                                val ry = kotlin.math.max(16f, kotlin.math.abs(curr.y - cy))
                                w = if (pendingShapeType == RecognizedShapeType.CIRCLE) rx * 2f else rx * 2f
                                h = if (pendingShapeType == RecognizedShapeType.CIRCLE) rx * 2f else ry * 2f
                            }

                            val newShape = CanvasShape(
                                id = "shape_${currentTimeMillis()}_${Random.nextInt(10000, 99999)}",
                                type = pendingShapeType.name,
                                x = cx,
                                y = cy,
                                width = if (w == 0f) 80f else w,
                                height = if (h == 0f) 80f else h,
                                colorHex = colorToHex(currentBrushState.color),
                                strokeWidth = currentBrushState.baseWidth,
                                lineStyle = "SOLID"
                            )

                            val updatedLayers = if (currentLayersState.isEmpty()) {
                                listOf(
                                    CanvasLayer(
                                        id = "layer_default",
                                        name = "Vector Layer 1",
                                        shapes = listOf(newShape)
                                    )
                                )
                            } else {
                                currentLayersState.mapIndexed { index, layer ->
                                    if (index == 0) layer.copy(shapes = layer.shapes + newShape) else layer
                                }
                            }
                            currentOnLayersChange(updatedLayers)
                            onShapePlaced?.invoke(newShape)
                            shapeDragStart = null
                            shapeDragCurrent = null
                        } else if (currentBrushState.toolType == ToolType.VECTOR_ERASER) {
                            eraseIntersectingStrokes(activePoints.toList(), currentLayersState, currentOnLayersChange, currentBrushState.baseWidth)
                            activePoints.clear()
                        } else if (activePoints.isNotEmpty()) {
                            // Freehand drawing preserves points as drawn (NO unintended shape auto-replacement)
                            val finalStrokePoints = if (autoSnap && activePoints.size >= 5) {
                                val detected = ShapeRecognizer.recognize(activePoints.map { it.offset })
                                detected?.toStrokePoints() ?: activePoints.toList()
                            } else {
                                activePoints.toList()
                            }

                            val effectiveWidth = currentBrushState.calculateEffectiveWidth(
                                pressure = activePoints.lastOrNull()?.pressure ?: 0.5f
                            )

                            // Monotonically unique, collision-free stroke ID
                            val newInkStroke = InkStroke(
                                id = "stroke_${currentTimeMillis()}_${Random.nextInt(10000, 99999)}",
                                tool = currentBrushState.toolType,
                                colorHex = colorToHex(currentBrushState.color),
                                strokeWidth = effectiveWidth,
                                opacity = currentBrushState.alpha,
                                points = finalStrokePoints.map { it.toInkPoint() }
                            )

                            val updatedLayers = if (currentLayersState.isEmpty()) {
                                listOf(
                                    CanvasLayer(
                                        id = "layer_default",
                                        name = "Vector Layer 1",
                                        layerType = LayerType.VECTOR,
                                        strokes = listOf(newInkStroke)
                                    )
                                )
                            } else {
                                currentLayersState.mapIndexed { index, layer ->
                                    if (index == 0) {
                                        layer.copy(strokes = layer.strokes + newInkStroke)
                                    } else {
                                        layer
                                    }
                                }
                            }
                            currentOnLayersChange(updatedLayers)
                            activePoints.clear()
                        }
                    },
                    onDragCancel = {
                        activePoints.clear()
                        shapeDragStart = null
                        shapeDragCurrent = null
                    }
                )
            }
    ) {
        // Main Drawing Canvas: Roll Background, Strokes, Shapes, and Live Previews
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width

            // 1. Continuous Page Roll Background & Dividers
            drawRect(color = canvasBg)

            var currentY = 0f
            val maxRollHeight = 6000f
            while (currentY < maxRollHeight) {
                currentY += pageHeight
                val screenY = currentY * scale + offset.y
                if (screenY in 0f..size.height) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, screenY),
                        end = Offset(canvasWidth, screenY),
                        strokeWidth = 2f
                    )
                }
            }

            // 2. Render Completed Vector Layers (Strokes and Shapes)
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, Offset.Zero)
            }) {
                layers.forEach { layer ->
                    if (layer.isVisible) {
                        // Draw vector strokes
                        layer.strokes.forEach { stroke ->
                            val path = CatmullRomConverter.inkPointsToCubicPath(stroke.points, stroke.strokeWidth)
                            val isHighlighter = stroke.tool == ToolType.HIGHLIGHTER
                            val strokeColor = parseColorHex(stroke.colorHex).copy(
                                alpha = (if (isHighlighter) 0.35f else stroke.opacity) * layer.opacity
                            )
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = if (isHighlighter) BlendMode.Darken else BlendMode.SrcOver
                            )
                        }

                        // Draw vector shapes
                        layer.shapes.forEach { shape ->
                            drawCanvasShape(shape, layer.opacity)
                        }
                    }
                }

                // 3. Render Active In-Progress Stroke
                if (activePoints.isNotEmpty() && currentBrush.toolType != ToolType.VECTOR_ERASER) {
                    val activePath = CatmullRomConverter.pointsToCubicPath(activePoints, currentBrush.baseWidth)
                    val effectiveWidth = currentBrush.calculateEffectiveWidth(
                        pressure = activePoints.lastOrNull()?.pressure ?: 0.5f
                    )
                    drawPath(
                        path = activePath,
                        color = currentBrush.color.copy(alpha = currentBrush.alpha),
                        style = Stroke(
                            width = effectiveWidth,
                            cap = currentBrush.strokeCap,
                            join = currentBrush.strokeJoin
                        ),
                        blendMode = currentBrush.blendMode
                    )
                }

                // 4. Render Live Shape Placement Preview
                if (pendingShapeType != null && shapeDragStart != null && shapeDragCurrent != null) {
                    val start = shapeDragStart!!
                    val curr = shapeDragCurrent!!
                    val cx: Float
                    val cy: Float
                    val w: Float
                    val h: Float
                    if (pendingShapeType == RecognizedShapeType.STRAIGHT_LINE) {
                        cx = (start.x + curr.x) / 2f
                        cy = (start.y + curr.y) / 2f
                        w = curr.x - start.x
                        h = curr.y - start.y
                    } else {
                        cx = start.x
                        cy = start.y
                        val rx = kotlin.math.max(16f, kotlin.math.abs(curr.x - cx))
                        val ry = kotlin.math.max(16f, kotlin.math.abs(curr.y - cy))
                        w = if (pendingShapeType == RecognizedShapeType.CIRCLE) rx * 2f else rx * 2f
                        h = if (pendingShapeType == RecognizedShapeType.CIRCLE) rx * 2f else ry * 2f
                    }
                    val previewShape = CanvasShape(
                        id = "preview",
                        type = pendingShapeType.name,
                        x = cx,
                        y = cy,
                        width = w,
                        height = h,
                        colorHex = colorToHex(currentBrush.color),
                        strokeWidth = currentBrush.baseWidth,
                        lineStyle = "DASHED"
                    )
                    drawCanvasShape(previewShape, opacity = 0.8f)
                }
            }
        }

        // 5. Shape Placement Hint Banner
        if (pendingShapeType != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Touch center & drag to size ${pendingShapeType.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = { onCancelShapePlacement?.invoke() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 6. Interactive Text Containers
        layers.flatMap { it.textBoxes }.forEach { textBox ->
            val screenX = textBox.x * scale + offset.x
            val screenY = textBox.y * scale + offset.y
            val screenW = (textBox.width * scale).coerceAtLeast(140f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                    .width(screenW.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    ),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        // Drag header + Delete button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(textBox.id, scale) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        updateTextBoxInLayers(
                                            textBox.copy(
                                                x = textBox.x + (dragAmount.x / scale),
                                                y = textBox.y + (dragAmount.y / scale)
                                            )
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag Text Box",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Text",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { deleteTextBoxFromLayers(textBox.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete Text",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Editable Text Field
                        BasicTextField(
                            value = textBox.text,
                            onValueChange = { newText ->
                                updateTextBoxInLayers(textBox.copy(text = newText))
                            },
                            textStyle = TextStyle(
                                fontSize = textBox.fontSize.sp,
                                color = parseColorHex(textBox.colorHex),
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // 7. Samsung Notes-Style Selected Shape Manipulation Overlay
        val activeSelectedShape = layers.flatMap { it.shapes }.firstOrNull { it.id == selectedShapeId }
        if (activeSelectedShape != null) {
            val screenCenterX = activeSelectedShape.x * scale + offset.x
            val screenCenterY = activeSelectedShape.y * scale + offset.y
            val screenHalfW = (kotlin.math.abs(activeSelectedShape.width) * scale / 2f).coerceAtLeast(16f)
            val screenHalfH = (kotlin.math.abs(activeSelectedShape.height) * scale / 2f).coerceAtLeast(16f)
            val boxLeft = screenCenterX - screenHalfW
            val boxTop = screenCenterY - screenHalfH
            val boxWidth = screenHalfW * 2f
            val boxHeight = screenHalfH * 2f

            // Bounding Box with Move Drag Gesture
            Box(
                modifier = Modifier
                    .offset { IntOffset(boxLeft.roundToInt(), boxTop.roundToInt()) }
                    .size(boxWidth.dp, boxHeight.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .pointerInput(activeSelectedShape.id, scale) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            updateShapeInLayers(
                                activeSelectedShape.copy(
                                    x = activeSelectedShape.x + (dragAmount.x / scale),
                                    y = activeSelectedShape.y + (dragAmount.y / scale)
                                )
                            )
                        }
                    }
            )

            // Corner Resize Handles
            ResizeHandle(
                center = Offset(boxLeft, boxTop),
                onDragDelta = { delta ->
                    val dw = (-delta.x * 2f) / scale
                    val dh = (-delta.y * 2f) / scale
                    updateShapeInLayers(
                        activeSelectedShape.copy(
                            width = (activeSelectedShape.width + dw).coerceAtLeast(30f),
                            height = (activeSelectedShape.height + dh).coerceAtLeast(30f)
                        )
                    )
                }
            )
            ResizeHandle(
                center = Offset(boxLeft + boxWidth, boxTop + boxHeight),
                onDragDelta = { delta ->
                    val dw = (delta.x * 2f) / scale
                    val dh = (delta.y * 2f) / scale
                    updateShapeInLayers(
                        activeSelectedShape.copy(
                            width = (activeSelectedShape.width + dw).coerceAtLeast(30f),
                            height = (activeSelectedShape.height + dh).coerceAtLeast(30f)
                        )
                    )
                }
            )

            // Floating Contextual Action Bar (Samsung Notes Style)
            val barTop = (boxTop - 54f).coerceAtLeast(16f)
            Surface(
                modifier = Modifier
                    .offset { IntOffset(screenCenterX.roundToInt() - 140, barTop.roundToInt()) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Delete
                    IconButton(
                        onClick = { deleteShapeFromLayers(activeSelectedShape.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Shape",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Toggle Color Palette Cycle
                    IconButton(
                        onClick = {
                            val currentIndex = shapeColors.indexOfFirst { colorToHex(it) == activeSelectedShape.colorHex }
                            val nextColor = shapeColors[(currentIndex + 1).coerceAtLeast(0) % shapeColors.size]
                            updateShapeInLayers(activeSelectedShape.copy(colorHex = colorToHex(nextColor)))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(activeSelectedShape.colorHex))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                    }

                    // Stroke Thickness Toggle (2dp -> 4dp -> 8dp -> 2dp)
                    IconButton(
                        onClick = {
                            val nextWidth = when {
                                activeSelectedShape.strokeWidth < 3f -> 4.0f
                                activeSelectedShape.strokeWidth < 6f -> 8.0f
                                else -> 2.0f
                            }
                            updateShapeInLayers(activeSelectedShape.copy(strokeWidth = nextWidth))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            "${activeSelectedShape.strokeWidth.toInt()}pt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Line Style Toggle: Solid <-> Dashed
                    IconButton(
                        onClick = {
                            val nextStyle = if (activeSelectedShape.lineStyle == "SOLID") "DASHED" else "SOLID"
                            updateShapeInLayers(activeSelectedShape.copy(lineStyle = nextStyle))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (activeSelectedShape.lineStyle == "DASHED") Icons.Default.LinearScale else Icons.Default.HorizontalRule,
                            contentDescription = "Toggle Line Style",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Done / Deselect
                    IconButton(
                        onClick = { selectedShapeId = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(
    center: Offset,
    onDragDelta: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((center.x - 10f).roundToInt(), (center.y - 10f).roundToInt()) }
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount)
                }
            }
    )
}

private fun DrawScope.drawCanvasShape(shape: CanvasShape, opacity: Float = 1.0f) {
    val color = parseColorHex(shape.colorHex).copy(alpha = opacity)
    val pathEffect = if (shape.lineStyle == "DASHED") PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f) else null
    val strokeStyle = Stroke(
        width = shape.strokeWidth,
        pathEffect = pathEffect,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val hw = kotlin.math.abs(shape.width) / 2f
    val hh = kotlin.math.abs(shape.height) / 2f

    when (shape.type) {
        "STRAIGHT_LINE" -> {
            drawLine(
                color = color,
                start = Offset(shape.x - hw, shape.y - hh),
                end = Offset(shape.x + hw, shape.y + hh),
                strokeWidth = shape.strokeWidth,
                pathEffect = pathEffect,
                cap = StrokeCap.Round
            )
        }
        "RECTANGLE" -> {
            drawRect(
                color = color,
                topLeft = Offset(shape.x - hw, shape.y - hh),
                size = Size(hw * 2f, hh * 2f),
                style = strokeStyle
            )
        }
        "CIRCLE" -> {
            drawCircle(
                color = color,
                radius = hw,
                center = Offset(shape.x, shape.y),
                style = strokeStyle
            )
        }
        "ELLIPSE" -> {
            val path = Path().apply {
                addOval(Rect(shape.x - hw, shape.y - hh, shape.x + hw, shape.y + hh))
            }
            drawPath(path = path, color = color, style = strokeStyle)
        }
        "TRIANGLE" -> {
            val path = Path().apply {
                moveTo(shape.x, shape.y - hh)
                lineTo(shape.x + hw, shape.y + hh)
                lineTo(shape.x - hw, shape.y + hh)
                close()
            }
            drawPath(path = path, color = color, style = strokeStyle)
        }
    }
}

private fun eraseIntersectingStrokes(
    eraserPoints: List<StrokePoint>,
    layers: List<CanvasLayer>,
    onLayersChange: (List<CanvasLayer>) -> Unit,
    eraserBaseWidth: Float
) {
    val eraserRadius = eraserBaseWidth * 3.0f

    val updatedLayers = layers.map { layer ->
        val remainingStrokes = layer.strokes.filterNot { stroke ->
            stroke.points.any { sp ->
                eraserPoints.any { ep ->
                    hypot(ep.x - sp.x, ep.y - sp.y) < (eraserRadius + stroke.strokeWidth)
                }
            }
        }
        val remainingShapes = layer.shapes.filterNot { shape ->
            eraserPoints.any { ep ->
                val hw = kotlin.math.abs(shape.width) / 2f
                val hh = kotlin.math.abs(shape.height) / 2f
                ep.x in (shape.x - hw - eraserRadius)..(shape.x + hw + eraserRadius) &&
                        ep.y in (shape.y - hh - eraserRadius)..(shape.y + hh + eraserRadius)
            }
        }
        layer.copy(strokes = remainingStrokes, shapes = remainingShapes)
    }

    onLayersChange(updatedLayers)
}

private fun parseColorHex(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color((0xFF000000 or colorInt).toInt())
        } else {
            Color(colorInt.toInt())
        }
    } catch (_: Exception) {
        Color.Black
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    fun Int.toHex(): String = toString(16).padStart(2, '0').uppercase()
    return "#${r.toHex()}${g.toHex()}${b.toHex()}"
}
