package com.notes.client.canvas.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notes.client.canvas.spline.StrokePoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class RecognizedShapeType {
    STRAIGHT_LINE,
    RECTANGLE,
    CIRCLE,
    ELLIPSE,
    TRIANGLE
}

sealed class SnappedShape {
    abstract val shapeType: RecognizedShapeType
    abstract fun toStrokePoints(): List<StrokePoint>

    data class Line(val start: Offset, val end: Offset) : SnappedShape() {
        override val shapeType: RecognizedShapeType = RecognizedShapeType.STRAIGHT_LINE
        override fun toStrokePoints(): List<StrokePoint> = listOf(
            StrokePoint(start.x, start.y),
            StrokePoint(end.x, end.y)
        )
    }

    data class Rectangle(val bounds: Rect) : SnappedShape() {
        override val shapeType: RecognizedShapeType = RecognizedShapeType.RECTANGLE
        override fun toStrokePoints(): List<StrokePoint> = listOf(
            StrokePoint(bounds.left, bounds.top),
            StrokePoint(bounds.right, bounds.top),
            StrokePoint(bounds.right, bounds.bottom),
            StrokePoint(bounds.left, bounds.bottom),
            StrokePoint(bounds.left, bounds.top) // Closed
        )
    }

    data class Circle(val center: Offset, val radius: Float) : SnappedShape() {
        override val shapeType: RecognizedShapeType = RecognizedShapeType.CIRCLE
        override fun toStrokePoints(): List<StrokePoint> {
            val points = mutableListOf<StrokePoint>()
            val steps = 36
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                points.add(StrokePoint(x, y))
            }
            return points
        }
    }

    data class Ellipse(val center: Offset, val radiusX: Float, val radiusY: Float) : SnappedShape() {
        override val shapeType: RecognizedShapeType = RecognizedShapeType.ELLIPSE
        override fun toStrokePoints(): List<StrokePoint> {
            val points = mutableListOf<StrokePoint>()
            val steps = 36
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
                val x = center.x + radiusX * cos(angle)
                val y = center.y + radiusY * sin(angle)
                points.add(StrokePoint(x, y))
            }
            return points
        }
    }

    data class Triangle(val p1: Offset, val p2: Offset, val p3: Offset) : SnappedShape() {
        override val shapeType: RecognizedShapeType = RecognizedShapeType.TRIANGLE
        override fun toStrokePoints(): List<StrokePoint> = listOf(
            StrokePoint(p1.x, p1.y),
            StrokePoint(p2.x, p2.y),
            StrokePoint(p3.x, p3.y),
            StrokePoint(p1.x, p1.y) // Closed
        )
    }
}

object ShapeRecognizer {

    /**
     * Recognizes geometric shapes from a sequence of touch coordinates.
     */
    fun recognize(points: List<Offset>): SnappedShape? {
        if (points.size < 5) return null

        val start = points.first()
        val end = points.last()
        val distStartEnd = hypot(end.x - start.x, end.y - start.y)

        // 1. Calculate cumulative path length along stroke
        var totalPathLength = 0f
        for (i in 0 until points.size - 1) {
            totalPathLength += hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        }
        if (totalPathLength <= 1f) return null

        // 2. Check for Straight Line
        if (distStartEnd / totalPathLength >= 0.88f) {
            return SnappedShape.Line(start, end)
        }

        // 3. Compute Bounding Box
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width < 5f || height < 5f) return null

        val bounds = Rect(minX, minY, maxX, maxY)
        val maxExtent = width.coerceAtLeast(height)
        val isClosed = distStartEnd < (maxExtent * 0.35f)

        if (!isClosed) return null

        val center = bounds.center
        val rx = width / 2f
        val ry = height / 2f

        // 4. Test for Circle / Ellipse: Normalized radial error variance & max corner excursion
        var radialErrorSum = 0f
        var maxRadialError = 0f
        points.forEach { pt ->
            val normDist = sqrt(((pt.x - center.x) / rx).pow(2) + ((pt.y - center.y) / ry).pow(2))
            val err = abs(normDist - 1.0f)
            if (err > maxRadialError) maxRadialError = err
            radialErrorSum += err.pow(2)
        }
        val meanRadialError = radialErrorSum / points.size

        if (meanRadialError < 0.06f && maxRadialError < 0.25f) {
            val aspectRatio = width / height
            return if (aspectRatio in 0.85f..1.15f) {
                val avgRadius = (rx + ry) / 2f
                SnappedShape.Circle(center, avgRadius)
            } else {
                SnappedShape.Ellipse(center, rx, ry)
            }
        }

        // 5. Test for Rectangle: Perimeter match
        val perimeter = 2f * (width + height)
        if (abs(totalPathLength - perimeter) / perimeter < 0.25f) {
            return SnappedShape.Rectangle(bounds)
        }

        // 6. Test for Triangle: Top peak and bottom two corners
        return SnappedShape.Triangle(
            p1 = Offset(center.x, bounds.top),
            p2 = Offset(bounds.right, bounds.bottom),
            p3 = Offset(bounds.left, bounds.bottom)
        )
    }

    /**
     * Creates a default centered primitive for insertion via toolbar.
     */
    fun createDefaultPrimitive(type: RecognizedShapeType, center: Offset): SnappedShape {
        return when (type) {
            RecognizedShapeType.STRAIGHT_LINE -> SnappedShape.Line(
                start = Offset(center.x - 120f, center.y),
                end = Offset(center.x + 120f, center.y)
            )
            RecognizedShapeType.RECTANGLE -> SnappedShape.Rectangle(
                bounds = Rect(
                    center.x - 100f,
                    center.y - 60f,
                    center.x + 100f,
                    center.y + 60f
                )
            )
            RecognizedShapeType.CIRCLE -> SnappedShape.Circle(
                center = center,
                radius = 70f
            )
            RecognizedShapeType.ELLIPSE -> SnappedShape.Ellipse(
                center = center,
                radiusX = 110f,
                radiusY = 60f
            )
            RecognizedShapeType.TRIANGLE -> SnappedShape.Triangle(
                p1 = Offset(center.x, center.y - 70f),
                p2 = Offset(center.x + 80f, center.y + 60f),
                p3 = Offset(center.x - 80f, center.y + 60f)
            )
        }
    }

    /**
     * Creates a shape dynamically from touch start (center) and current drag offset.
     */
    fun createPrimitiveFromDrag(type: RecognizedShapeType, start: Offset, current: Offset): SnappedShape {
        return when (type) {
            RecognizedShapeType.STRAIGHT_LINE -> SnappedShape.Line(
                start = start,
                end = current
            )
            RecognizedShapeType.RECTANGLE -> {
                val rx = kotlin.math.max(10f, kotlin.math.abs(current.x - start.x))
                val ry = kotlin.math.max(10f, kotlin.math.abs(current.y - start.y))
                SnappedShape.Rectangle(
                    bounds = Rect(start.x - rx, start.y - ry, start.x + rx, start.y + ry)
                )
            }
            RecognizedShapeType.CIRCLE -> {
                val r = kotlin.math.max(10f, hypot(current.x - start.x, current.y - start.y))
                SnappedShape.Circle(
                    center = start,
                    radius = r
                )
            }
            RecognizedShapeType.ELLIPSE -> {
                val rx = kotlin.math.max(10f, kotlin.math.abs(current.x - start.x))
                val ry = kotlin.math.max(10f, kotlin.math.abs(current.y - start.y))
                SnappedShape.Ellipse(
                    center = start,
                    radiusX = rx,
                    radiusY = ry
                )
            }
            RecognizedShapeType.TRIANGLE -> {
                val rx = kotlin.math.max(10f, kotlin.math.abs(current.x - start.x))
                val ry = kotlin.math.max(10f, kotlin.math.abs(current.y - start.y))
                SnappedShape.Triangle(
                    p1 = Offset(start.x, start.y - ry),
                    p2 = Offset(start.x + rx, start.y + ry),
                    p3 = Offset(start.x - rx, start.y + ry)
                )
            }
        }
    }

    /**
     * Converts a CanvasShape data model into a SnappedShape for rendering.
     */
    fun canvasShapeToSnapped(shape: com.notes.common.models.CanvasShape): SnappedShape {
        val center = Offset(shape.x, shape.y)
        val hw = shape.width / 2f
        val hh = shape.height / 2f
        return when (shape.type) {
            RecognizedShapeType.STRAIGHT_LINE.name -> SnappedShape.Line(
                start = Offset(shape.x - hw, shape.y - hh),
                end = Offset(shape.x + hw, shape.y + hh)
            )
            RecognizedShapeType.RECTANGLE.name -> SnappedShape.Rectangle(
                bounds = Rect(shape.x - hw, shape.y - hh, shape.x + hw, shape.y + hh)
            )
            RecognizedShapeType.CIRCLE.name -> SnappedShape.Circle(
                center = center,
                radius = hw
            )
            RecognizedShapeType.ELLIPSE.name -> SnappedShape.Ellipse(
                center = center,
                radiusX = hw,
                radiusY = hh
            )
            RecognizedShapeType.TRIANGLE.name -> SnappedShape.Triangle(
                p1 = Offset(center.x, center.y - hh),
                p2 = Offset(center.x + hw, center.y + hh),
                p3 = Offset(center.x - hw, center.y + hh)
            )
            else -> SnappedShape.Rectangle(
                bounds = Rect(shape.x - hw, shape.y - hh, shape.x + hw, shape.y + hh)
            )
        }
    }
}

