package com.notes.client.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.shapes.ShapeRecognizer
import com.notes.client.canvas.spline.CatmullRomConverter
import com.notes.client.canvas.spline.StrokePoint
import com.notes.common.models.CanvasLayer
import com.notes.common.models.InkStroke
import com.notes.common.models.LayerType
import com.notes.common.models.ToolType
import kotlin.math.hypot

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
    autoSnap: Boolean = true,
    pageHeight: Float = 1200f
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Active in-progress stroke sampled points
    val activePoints = remember { mutableStateListOf<StrokePoint>() }

    // Multi-touch pinch-to-zoom and pan state
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3.0f)
        offset += panChange
    }

    // Build cached renderable strokes per layer
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

    var strokeCounter by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(currentBrush, scale, offset, autoSnap) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val localPoint = (startOffset - offset) / scale
                        activePoints.clear()
                        activePoints.add(
                            StrokePoint(
                                x = localPoint.x,
                                y = localPoint.y,
                                pressure = 0.5f,
                                timestamp = 0L
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        val localPoint = (change.position - offset) / scale
                        val pressure = change.pressure.coerceIn(0.1f, 1.0f)
                        activePoints.add(
                            StrokePoint(
                                x = localPoint.x,
                                y = localPoint.y,
                                pressure = pressure,
                                timestamp = 0L
                            )
                        )
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentBrush.toolType == ToolType.VECTOR_ERASER) {
                            // Perform vector eraser intersection
                            eraseIntersectingStrokes(activePoints.toList(), layers, onLayersChange, currentBrush.baseWidth)
                        } else if (activePoints.size >= 1) {
                            // Recognize geometric shapes if autoSnap enabled and valid contour drawn
                            val finalStrokePoints = if (autoSnap && activePoints.size >= 5) {
                                val detected = ShapeRecognizer.recognize(activePoints.map { it.offset })
                                detected?.toStrokePoints() ?: activePoints.toList()
                            } else {
                                activePoints.toList()
                            }

                            // Append new stroke to active vector layer
                            val effectiveWidth = currentBrush.calculateEffectiveWidth(
                                pressure = activePoints.lastOrNull()?.pressure ?: 0.5f
                            )
                            strokeCounter++
                            val newInkStroke = InkStroke(
                                id = "stroke_$strokeCounter",
                                tool = currentBrush.toolType,
                                colorHex = colorToHex(currentBrush.color),
                                strokeWidth = effectiveWidth,
                                opacity = currentBrush.alpha,
                                points = finalStrokePoints.map { it.toInkPoint() }
                            )

                            val updatedLayers = if (layers.isEmpty()) {
                                listOf(
                                    CanvasLayer(
                                        id = "layer_default",
                                        name = "Vector Layer 1",
                                        layerType = LayerType.VECTOR,
                                        strokes = listOf(newInkStroke)
                                    )
                                )
                            } else {
                                layers.mapIndexed { index, layer ->
                                    if (index == 0) {
                                        layer.copy(strokes = layer.strokes + newInkStroke)
                                    } else {
                                        layer
                                    }
                                }
                            }
                            onLayersChange(updatedLayers)
                        }
                        activePoints.clear()
                    },
                    onDragCancel = {
                        activePoints.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width

            // 1. Draw Continuous Page Roll Background & Dividers
            drawRect(color = canvasBg)

            var currentY = 0f
            val maxRollHeight = 6000f
            var pageIndex = 1
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
                pageIndex++
            }

            // 2. Render Completed Vector Layers in Order
            renderableLayers.forEach { (layer, strokes) ->
                if (layer.isVisible) {
                    strokes.forEach { renderable ->
                        drawRenderableStroke(renderable, offset, scale, layer.opacity)
                    }
                }
            }

            // 3. Render Active In-Progress Stroke with Low-Latency
            if (activePoints.isNotEmpty() && currentBrush.toolType != ToolType.VECTOR_ERASER) {
                val activePath = CatmullRomConverter.pointsToCubicPath(activePoints, currentBrush.baseWidth)
                val effectiveWidth = currentBrush.calculateEffectiveWidth(
                    pressure = activePoints.lastOrNull()?.pressure ?: 0.5f
                ) * scale

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
        }
    }
}

private fun DrawScope.drawRenderableStroke(
    renderable: RenderableStroke,
    offset: Offset,
    scale: Float,
    layerOpacity: Float
) {
    drawPath(
        path = renderable.cachedPath,
        color = renderable.brush.color.copy(alpha = renderable.brush.alpha * layerOpacity),
        style = Stroke(
            width = renderable.stroke.strokeWidth * scale,
            cap = renderable.brush.strokeCap,
            join = renderable.brush.strokeJoin
        ),
        blendMode = renderable.brush.blendMode
    )
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
            // Stroke-level hit testing
            stroke.points.any { sp ->
                eraserPoints.any { ep ->
                    hypot(ep.x - sp.x, ep.y - sp.y) < (eraserRadius + stroke.strokeWidth)
                }
            }
        }
        layer.copy(strokes = remainingStrokes)
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
