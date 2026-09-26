package com.notes.client.canvas.spline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.notes.common.models.InkPoint

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val timestamp: Long = 0L
) {
    val offset: Offset get() = Offset(x, y)

    fun toInkPoint(): InkPoint = InkPoint(
        x = x,
        y = y,
        pressure = pressure,
        tilt = tilt,
        timestamp = timestamp
    )

    companion object {
        fun fromInkPoint(inkPoint: InkPoint): StrokePoint = StrokePoint(
            x = inkPoint.x,
            y = inkPoint.y,
            pressure = inkPoint.pressure,
            tilt = inkPoint.tilt,
            timestamp = inkPoint.timestamp
        )
    }
}

data class SplineSegment(
    val p1: StrokePoint,
    val c1: Offset,
    val c2: Offset,
    val p2: StrokePoint
)

object CatmullRomConverter {

    /**
     * Calculates mathematical Catmull-Rom cubic Bézier control points for every segment
     * with boundary ghost point synthesis.
     */
    fun calculateSplineSegments(points: List<StrokePoint>): List<SplineSegment> {
        if (points.size < 2) return emptyList()

        val segments = mutableListOf<SplineSegment>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val p0 = if (i > 0) {
                points[i - 1]
            } else {
                StrokePoint(
                    x = 2f * p1.x - p2.x,
                    y = 2f * p1.y - p2.y,
                    pressure = p1.pressure,
                    tilt = p1.tilt
                )
            }

            val p3 = if (i + 2 < points.size) {
                points[i + 2]
            } else {
                StrokePoint(
                    x = 2f * p2.x - p1.x,
                    y = 2f * p2.y - p1.y,
                    pressure = p2.pressure,
                    tilt = p2.tilt
                )
            }

            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            segments.add(
                SplineSegment(
                    p1 = p1,
                    c1 = Offset(c1x, c1y),
                    c2 = Offset(c2x, c2y),
                    p2 = p2
                )
            )
        }
        return segments
    }

    /**
     * Converts a raw list of sampled points into a smooth Compose Multiplatform Path
     * using cubic Bézier segments derived from Catmull-Rom splines.
     */
    fun pointsToCubicPath(points: List<StrokePoint>, baseWidth: Float = 4f): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points.first()
            val radius = (baseWidth / 2f).coerceAtLeast(1f)
            path.addOval(
                Rect(
                    left = p.x - radius,
                    top = p.y - radius,
                    right = p.x + radius,
                    bottom = p.y + radius
                )
            )
            return path
        }

        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        val segments = calculateSplineSegments(points)
        path.moveTo(points[0].x, points[0].y)
        for (segment in segments) {
            path.cubicTo(
                segment.c1.x,
                segment.c1.y,
                segment.c2.x,
                segment.c2.y,
                segment.p2.x,
                segment.p2.y
            )
        }

        return path
    }

    /**
     * Helper to convert common-model InkPoints directly to cubic path.
     */
    fun inkPointsToCubicPath(points: List<InkPoint>, baseWidth: Float = 4f): Path {
        return pointsToCubicPath(points.map { StrokePoint.fromInkPoint(it) }, baseWidth)
    }
}
