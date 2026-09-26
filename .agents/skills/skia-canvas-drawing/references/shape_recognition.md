# Geometric Shape Recognition and Auto-Snapping

This document details shape detection, fitting, and the 0.5-second draw-and-hold snapping gesture (per Samsung Notes reference and architectural decision Q13).

## 1. Supported Shapes

The canvas supports two modalities for shapes:
1. **Toolbar Shape Tool**: Explicit insertion of Rectangle, Oval/Circle, Straight Line, Wavy Line.
2. **Auto-Snapping Gesture**: Drawing a rough shape and holding stylus/finger still for $\ge 500\text{ ms}$.

Supported snapped geometries:
- **Straight Line**: Segment connecting start and end points.
- **Rectangle**: Orthogonal 4-corner polygon bounding the drawn path.
- **Circle / Oval (Ellipse)**: Fitted ellipse based on bounding box and radial variance.
- **Wavy Line**: Regularized sine wave or smoothed spline line.
- **Triangle / Polygon**: Convex hull simplification with sharp vertices.

---

## 2. Draw-and-Hold Gesture Detection

### Thresholds
- **Hold Duration**: 500 ms ($0.5\text{ s}$).
- **Spatial Movement Window**: Displacement of less than $5\text{ dp}$ in Euclidean distance between samples during the final 500 ms.
- **Speed Decay**: Touch velocity near zero.

### Finite State Machine
1. `DRAWING`: Points stream in, rendered live using Catmull-Rom spline.
2. `HOLDING_DETECTED`: Velocity drops below threshold; timer launched for 500 ms.
   - If user moves $> 5\text{ dp}$, timer cancels, state returns to `DRAWING`.
3. `SNAP_TRIGGERED`: After 500 ms stationary, analyze point history and trigger shape recognition.
4. `SNAPPED`: Replace raw stroke points with regularized geometric shape, triggering haptic feedback.

---

## 3. Shape Classification Algorithm

```kotlin
package com.notes.client.canvas.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.*

enum class RecognizedShapeType {
    STRAIGHT_LINE,
    RECTANGLE,
    CIRCLE_OR_ELLIPSE,
    TRIANGLE,
    UNKNOWN
}

sealed class SnappedShape {
    data class Line(val start: Offset, val end: Offset) : SnappedShape()
    data class Rectangle(val rect: Rect) : SnappedShape()
    data class Circle(val center: Offset, val radius: Float) : SnappedShape()
    data class Ellipse(val bounds: Rect) : SnappedShape()
}

object ShapeRecognizer {

    /**
     * Classifies a sequence of stroke points into a canonical geometric shape.
     */
    fun recognize(points: List<Offset>): SnappedShape? {
        if (points.size < 5) return null

        val start = points.first()
        val end = points.last()
        val distanceStartEnd = (end - start).getDistance()

        // 1. Calculate path length along the stroke
        var totalPathLength = 0f
        for (i in 0 until points.size - 1) {
            totalPathLength += (points[i + 1] - points[i]).getDistance()
        }

        // 2. Check for Straight Line
        // If distance between start and end is >= 90% of total path length
        if (distanceStartEnd / totalPathLength >= 0.90f) {
            return SnappedShape.Line(start, end)
        }

        // 3. Compute Bounding Box
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
        val bounds = Rect(minX, minY, maxX, maxY)
        val isClosed = distanceStartEnd < (bounds.width.coerceAtLeast(bounds.height) * 0.25f)

        if (isClosed) {
            val center = bounds.center
            val radiusX = bounds.width / 2f
            val radiusY = bounds.height / 2f

            // Test for Circle / Ellipse: Radial variance
            var radialVarianceSum = 0f
            points.forEach { pt ->
                val normalizedDistance = sqrt(
                    ((pt.x - center.x) / radiusX).pow(2) +
                    ((pt.y - center.y) / radiusY).pow(2)
                )
                radialVarianceSum += (normalizedDistance - 1.0f).pow(2)
            }
            val meanRadialError = radialVarianceSum / points.size

            if (meanRadialError < 0.08f) {
                // If aspect ratio is close to 1:1, snap to perfect circle
                val aspectRatio = bounds.width / bounds.height
                return if (aspectRatio in 0.85f..1.15f) {
                    val avgRadius = (radiusX + radiusY) / 2f
                    SnappedShape.Circle(center, avgRadius)
                } else {
                    SnappedShape.Ellipse(bounds)
                }
            }

            // Test for Rectangle: Corner count & box perimeter ratio
            val perimeter = 2 * (bounds.width + bounds.height)
            if (abs(totalPathLength - perimeter) / perimeter < 0.20f) {
                return SnappedShape.Rectangle(bounds)
            }
        }

        return null
    }
}
```
