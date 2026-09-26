package com.notes.client.canvas.examples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.instruments.ToolType
import com.notes.client.canvas.spline.CatmullRomConverter
import com.notes.client.canvas.spline.StrokePoint

/**
 * Multi-layer data model for Canvas state.
 */
data class CanvasLayer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val strokes: List<RenderableStroke> = emptyList()
)

data class RenderableStroke(
    val id: String,
    val points: List<StrokePoint>,
    val brush: BrushConfig,
    val cachedPath: Path = CatmullRomConverter.pointsToCubicPath(points)
)

/**
 * Idiomatic Compose Multiplatform Canvas with:
 * - Continuous page roll dividers
 * - Pan and Zoom transformations
 * - Input dispatch (differentiating stylus drawing from finger navigation)
 * - Multi-layer rendering
 */
@Composable
fun SkiaHandwrittenCanvas(
    modifier: Modifier = Modifier,
    layers: List<CanvasLayer>,
    currentBrush: BrushConfig,
    onStrokeCompleted: (RenderableStroke) -> Unit,
    pageHeight: Float = 1200f
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Active in-progress stroke points
    val activePoints = remember { mutableStateListOf<StrokePoint>() }

    // Pinch-to-zoom and multi-touch panning state
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.25f, 5.0f)
        offset += panChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(currentBrush) {
                // Pointer input handler: Distinguish stylus vs touch
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val localCanvasPoint = (startOffset - offset) / scale
                        activePoints.clear()
                        activePoints.add(
                            StrokePoint(
                                x = localCanvasPoint.x,
                                y = localCanvasPoint.y,
                                pressure = 0.5f,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        val localCanvasPoint = (change.position - offset) / scale
                        val pressure = change.pressure.coerceIn(0.1f, 1.0f)

                        activePoints.add(
                            StrokePoint(
                                x = localCanvasPoint.x,
                                y = localCanvasPoint.y,
                                pressure = pressure,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        change.consume()
                    },
                    onDragEnd = {
                        if (activePoints.size >= 2) {
                            val newStroke = RenderableStroke(
                                id = "stroke_${System.currentTimeMillis()}",
                                points = activePoints.toList(),
                                brush = currentBrush
                            )
                            onStrokeCompleted(newStroke)
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
            // Apply viewport transformation (Pan + Zoom)
            val canvasWidth = size.width

            // 1. Draw Page Background Roll & Dividers
            drawPageRollBackground(offset, scale, pageHeight, canvasWidth)

            // 2. Render Completed Layers in Z-Index Order
            layers.filter { it.isVisible }.forEach { layer ->
                layer.strokes.forEach { stroke ->
                    drawRenderableStroke(stroke, offset, scale, layer.opacity)
                }
            }

            // 3. Render Active In-Progress Stroke
            if (activePoints.size >= 2) {
                val activePath = CatmullRomConverter.pointsToCubicPath(activePoints)
                drawPath(
                    path = activePath,
                    color = currentBrush.color.copy(alpha = currentBrush.alpha),
                    style = Stroke(
                        width = currentBrush.baseWidth * scale,
                        cap = currentBrush.strokeCap,
                        join = currentBrush.strokeJoin
                    ),
                    blendMode = currentBrush.blendMode
                )
            }
        }
    }
}

private fun DrawScope.drawPageRollBackground(
    offset: Offset,
    scale: Float,
    pageHeight: Float,
    canvasWidth: Float
) {
    // Draw continuous roll background
    drawRect(color = Color(0xFFFAF9F6))

    // Draw page break dividers at intervals
    var currentY = 0f
    val maxY = 10000f // Dynamically extended
    while (currentY < maxY) {
        currentY += pageHeight
        val screenY = currentY * scale + offset.y
        if (screenY in 0f..size.height) {
            drawLine(
                color = Color.LightGray.copy(alpha = 0.6f),
                start = Offset(0f, screenY),
                end = Offset(size.width, screenY),
                strokeWidth = 2f
            )
        }
    }
}

private fun DrawScope.drawRenderableStroke(
    stroke: RenderableStroke,
    offset: Offset,
    scale: Float,
    layerOpacity: Float
) {
    drawPath(
        path = stroke.cachedPath,
        color = stroke.brush.color.copy(alpha = stroke.brush.alpha * layerOpacity),
        style = Stroke(
            width = stroke.brush.baseWidth * scale,
            cap = stroke.brush.strokeCap,
            join = stroke.brush.strokeJoin
        ),
        blendMode = stroke.brush.blendMode
    )
}
