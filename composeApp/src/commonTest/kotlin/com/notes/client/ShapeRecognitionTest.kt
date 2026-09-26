package com.notes.client

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notes.client.canvas.shapes.RecognizedShapeType
import com.notes.client.canvas.shapes.ShapeRecognizer
import com.notes.client.canvas.shapes.SnappedShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShapeRecognitionTest {

    @Test
    fun testStraightLineRecognition() {
        val linePoints = listOf(
            Offset(0f, 0f),
            Offset(20f, 2f),
            Offset(40f, -1f),
            Offset(60f, 1f),
            Offset(80f, 0f),
            Offset(100f, 0f)
        )

        val recognized = ShapeRecognizer.recognize(linePoints)
        assertNotNull(recognized)
        assertTrue(recognized is SnappedShape.Line)
        assertEquals(RecognizedShapeType.STRAIGHT_LINE, recognized.shapeType)
        assertEquals(Offset(0f, 0f), (recognized as SnappedShape.Line).start)
        assertEquals(Offset(100f, 0f), recognized.end)
    }

    @Test
    fun testCircleRecognition() {
        val center = Offset(200f, 200f)
        val radius = 50f
        val circlePoints = mutableListOf<Offset>()
        for (i in 0..30) {
            val angle = (i.toFloat() / 30f) * (2f * PI.toFloat())
            // Add tiny noise
            val r = radius + (if (i % 2 == 0) 1.5f else -1.5f)
            circlePoints.add(Offset(center.x + r * cos(angle), center.y + r * sin(angle)))
        }

        val recognized = ShapeRecognizer.recognize(circlePoints)
        assertNotNull(recognized)
        assertTrue(recognized is SnappedShape.Circle || recognized is SnappedShape.Ellipse)
        val shapeType = recognized.shapeType
        assertTrue(shapeType == RecognizedShapeType.CIRCLE || shapeType == RecognizedShapeType.ELLIPSE)
    }

    @Test
    fun testRectangleRecognition() {
        val rectPoints = listOf(
            Offset(100f, 100f),
            Offset(150f, 100f),
            Offset(200f, 100f),
            Offset(200f, 130f),
            Offset(200f, 160f),
            Offset(150f, 160f),
            Offset(100f, 160f),
            Offset(100f, 130f),
            Offset(100f, 100f)
        )

        val recognized = ShapeRecognizer.recognize(rectPoints)
        assertNotNull(recognized)
        assertTrue(recognized is SnappedShape.Rectangle)
        assertEquals(RecognizedShapeType.RECTANGLE, recognized.shapeType)
    }

    @Test
    fun testTriangleRecognition() {
        val trianglePoints = listOf(
            Offset(150f, 50f),
            Offset(175f, 100f),
            Offset(200f, 150f),
            Offset(150f, 150f),
            Offset(100f, 150f),
            Offset(125f, 100f),
            Offset(150f, 50f)
        )

        val recognized = ShapeRecognizer.recognize(trianglePoints)
        assertNotNull(recognized)
        assertNotNull(recognized.shapeType)
    }

    @Test
    fun testShortPointsReturnsNull() {
        val shortList = listOf(Offset(0f, 0f), Offset(10f, 10f))
        val recognized = ShapeRecognizer.recognize(shortList)
        assertNull(recognized)
    }

    @Test
    fun testDefaultPrimitivesGeneration() {
        val center = Offset(300f, 300f)

        RecognizedShapeType.entries.forEach { type ->
            val primitive = ShapeRecognizer.createDefaultPrimitive(type, center)
            assertEquals(type, primitive.shapeType)
            val strokePoints = primitive.toStrokePoints()
            assertTrue(strokePoints.isNotEmpty(), "Primitive $type should produce stroke points")

            when (primitive) {
                is SnappedShape.Line -> assertEquals(2, strokePoints.size)
                is SnappedShape.Rectangle -> assertEquals(5, strokePoints.size)
                is SnappedShape.Triangle -> assertEquals(4, strokePoints.size)
                is SnappedShape.Circle -> assertTrue(strokePoints.size > 20)
                is SnappedShape.Ellipse -> assertTrue(strokePoints.size > 20)
            }
        }
    }
}
