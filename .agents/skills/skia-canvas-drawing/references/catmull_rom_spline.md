# Catmull-Rom Spline Interpolation for Ink Stroke Smoothing

This document outlines the mathematics and implementation of Catmull-Rom spline interpolation converted into cubic Bézier curves for hardware-accelerated Skia rendering.

## 1. Mathematical Foundation

A standard Catmull-Rom spline is a cubic Hermite spline where tangents at each point are determined using adjacent points. For a sequence of sampled points $P_0, P_1, P_2, \dots, P_n$:

The tangent $m_i$ at control point $P_i$ is defined as:
$$m_i = \frac{P_{i+1} - P_{i-1}}{2}$$

To render this spline on Skia / Compose Multiplatform without sampling hundreds of discrete micro-segments, we convert the Catmull-Rom segment between $P_1$ and $P_2$ into a standard cubic Bézier curve segment defined by four points:
- Start point: $P_1$
- Control point 1: $C_1 = P_1 + \frac{P_2 - P_0}{6}$
- Control point 2: $C_2 = P_2 - \frac{P_3 - P_1}{6}$
- End point: $P_2$

This conversion is exact and allows using native GPU path rendering via `Path.cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)`.

---

## 2. Boundary Conditions (Start & End of Stroke)

For strokes with $N$ points:
- When interpolating between $P_0$ and $P_1$, $P_{-1}$ does not exist. We synthesize $P_{-1} = 2P_0 - P_1$.
- When interpolating the final segment between $P_{N-2}$ and $P_{N-1}$, $P_N$ does not exist. We synthesize $P_N = 2P_{N-1} - P_{N-2}$.
- If a stroke has only 1 point: render a circle/dot with radius equal to `baseWidth / 2`.
- If a stroke has only 2 points: render a straight line or quadratic curve using midpoints.

---

## 3. Kotlin Implementation

```kotlin
package com.notes.client.canvas.spline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val timestamp: Long = 0L
) {
    val offset: Offset get() = Offset(x, y)
}

object CatmullRomConverter {
    /**
     * Converts a raw list of touch/stylus sample points into a smooth Compose Path
     * using cubic Bézier segments derived from Catmull-Rom splines.
     */
    fun pointsToCubicPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            val p = points.first()
            path.addOval(
                androidx.compose.ui.geometry.Rect(
                    center = p.offset,
                    radius = 2f * p.pressure
                )
            )
            return path
        }
        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        path.moveTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = if (i == 0) {
                // Synthesize P-1: 2*P0 - P1
                StrokePoint(
                    2 * points[0].x - points[1].x,
                    2 * points[0].y - points[1].y
                )
            } else {
                points[i - 1]
            }

            val p1 = points[i]
            val p2 = points[i + 1]

            val p3 = if (i + 2 < points.size) {
                points[i + 2]
            } else {
                // Synthesize PN: 2*P(N-1) - P(N-2)
                StrokePoint(
                    2 * points[i + 1].x - points[i].x,
                    2 * points[i + 1].y - points[i].y
                )
            }

            // Derive Bézier control points
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }

        return path
    }
}
```

---

## 4. Variable Width Segment Rendering

When pressure or velocity changes dynamically along the stroke, a single uniform `Stroke(width)` path is insufficient. In such cases:

1. **Polygon Outline Generation**: Compute normal vectors at each interpolated spline step and generate a closed boundary polygon filled with `Fill`.
2. **Skia Native Stroke Variation**: Use Skia `PathBuilder` with quad/cubic segments, modulating thickness between sampled anchors.
