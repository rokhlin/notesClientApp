package com.notes.client

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.spline.CatmullRomConverter
import com.notes.client.canvas.spline.StrokePoint
import com.notes.common.models.InkPoint
import com.notes.common.models.ToolType
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CanvasTest {

    @Test
    fun testCatmullRomSplineCalculation() {
        val points = listOf(
            StrokePoint(0f, 0f, 0.5f),
            StrokePoint(50f, 100f, 0.7f),
            StrokePoint(100f, 50f, 0.9f),
            StrokePoint(150f, 150f, 0.6f)
        )

        val segments = CatmullRomConverter.calculateSplineSegments(points)
        assertEquals(3, segments.size)

        // Segment 0 connects point 0 and point 1
        assertEquals(0f, segments[0].p1.x)
        assertEquals(50f, segments[0].p2.x)

        // Segment 1 connects point 1 and point 2
        assertEquals(50f, segments[1].p1.x)
        assertEquals(100f, segments[1].p2.x)

        // Verify control points are calculated
        assertNotNull(segments[0].c1)
        assertNotNull(segments[0].c2)
    }

    @Test
    fun testStrokePointInkPointRoundtrip() {
        val original = StrokePoint(
            x = 123.4f,
            y = 567.8f,
            pressure = 0.85f,
            tilt = 0.45f,
            timestamp = 1717000000L
        )

        val inkPoint = original.toInkPoint()
        assertEquals(original.x, inkPoint.x)
        assertEquals(original.y, inkPoint.y)
        assertEquals(original.pressure, inkPoint.pressure)
        assertEquals(original.tilt, inkPoint.tilt)
        assertEquals(original.timestamp, inkPoint.timestamp)

        val roundtrip = StrokePoint.fromInkPoint(inkPoint)
        assertEquals(original.x, roundtrip.x)
        assertEquals(original.y, roundtrip.y)
        assertEquals(original.pressure, roundtrip.pressure)
        assertEquals(original.tilt, roundtrip.tilt)
        assertEquals(original.timestamp, roundtrip.timestamp)
        assertEquals(Offset(123.4f, 567.8f), roundtrip.offset)
    }

    @Test
    fun testBrushConfigDefaultPresets() {
        ToolType.entries.forEach { toolType ->
            val config = BrushConfig.defaultFor(toolType, Color.Blue, baseWidth = 6f)
            assertEquals(toolType, config.toolType)
            assertEquals(6f, config.baseWidth)
            assertNotNull(config.strokeCap)
            assertNotNull(config.strokeJoin)
            assertNotNull(config.blendMode)
        }
    }

    @Test
    fun testBrushWidthPressureModulation() {
        val pen = BrushConfig.defaultFor(ToolType.PEN, baseWidth = 10f)
        val penLow = pen.calculateEffectiveWidth(pressure = 0.1f)
        val penHigh = pen.calculateEffectiveWidth(pressure = 1.0f)
        assertTrue(penHigh > penLow, "High pressure pen should produce thicker stroke than low pressure")

        val fountain = BrushConfig.defaultFor(ToolType.FOUNTAIN_PEN, baseWidth = 10f)
        val fountainLow = fountain.calculateEffectiveWidth(pressure = 0.1f)
        val fountainHigh = fountain.calculateEffectiveWidth(pressure = 1.0f)
        assertTrue(fountainHigh > fountainLow * 2, "Fountain pen should have high dynamic pressure range")

        val highlighter = BrushConfig.defaultFor(ToolType.HIGHLIGHTER, baseWidth = 10f)
        val highLow = highlighter.calculateEffectiveWidth(pressure = 0.1f)
        val highHigh = highlighter.calculateEffectiveWidth(pressure = 1.0f)
        assertEquals(highLow, highHigh, "Highlighter should have uniform marker width regardless of pressure")
        assertEquals(35f, highHigh, "Highlighter width should be 3.5x base width")

        val eraser = BrushConfig.defaultFor(ToolType.VECTOR_ERASER, baseWidth = 10f)
        assertEquals(30f, eraser.calculateEffectiveWidth(), "Eraser width should be 3.0x base width")
    }

    @Test
    fun testVectorEraserIntersectionLogic() {
        val eraserPoint = Offset(100f, 100f)
        val eraserRadius = 15f

        val strokePointInside = Offset(105f, 105f)
        val strokePointOutside = Offset(300f, 300f)

        val distInside = hypot(eraserPoint.x - strokePointInside.x, eraserPoint.y - strokePointInside.y)
        val distOutside = hypot(eraserPoint.x - strokePointOutside.x, eraserPoint.y - strokePointOutside.y)

        assertTrue(distInside < eraserRadius, "Point within radius should be detected for erasure")
        assertFalse(distOutside < eraserRadius, "Point far outside radius should not be erased")
    }
}
