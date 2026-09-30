package com.notes.client

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notes.client.canvas.export.CmnPackageSerializer
import com.notes.client.canvas.export.SvgExporter
import com.notes.client.canvas.shapes.RecognizedShapeType
import com.notes.client.canvas.shapes.ShapeRecognizer
import com.notes.client.canvas.shapes.SnappedShape
import com.notes.common.models.*
import kotlin.test.*

class CanvasEditorEnhancementsTest {

    @Test
    fun testShapeCreationViaDrag() {
        val center = Offset(200f, 300f)
        val dragPoint = Offset(260f, 380f)

        // 1. Rectangle
        val rect = ShapeRecognizer.createPrimitiveFromDrag(RecognizedShapeType.RECTANGLE, center, dragPoint)
        assertTrue(rect is SnappedShape.Rectangle)
        val rectBounds = rect.bounds
        assertEquals(140f, rectBounds.left) // 200 - 60
        assertEquals(260f, rectBounds.right) // 200 + 60
        assertEquals(220f, rectBounds.top) // 300 - 80
        assertEquals(380f, rectBounds.bottom) // 300 + 80

        // 2. Circle
        val circle = ShapeRecognizer.createPrimitiveFromDrag(RecognizedShapeType.CIRCLE, center, dragPoint)
        assertTrue(circle is SnappedShape.Circle)
        assertEquals(center, circle.center)
        assertEquals(100f, circle.radius) // hypot(60, 80) = 100

        // 3. Ellipse
        val ellipse = ShapeRecognizer.createPrimitiveFromDrag(RecognizedShapeType.ELLIPSE, center, dragPoint)
        assertTrue(ellipse is SnappedShape.Ellipse)
        assertEquals(center, ellipse.center)
        assertEquals(60f, ellipse.radiusX)
        assertEquals(80f, ellipse.radiusY)

        // 4. Straight Line
        val line = ShapeRecognizer.createPrimitiveFromDrag(RecognizedShapeType.STRAIGHT_LINE, center, dragPoint)
        assertTrue(line is SnappedShape.Line)
        assertEquals(center, line.start)
        assertEquals(dragPoint, line.end)

        // 5. Triangle
        val triangle = ShapeRecognizer.createPrimitiveFromDrag(RecognizedShapeType.TRIANGLE, center, dragPoint)
        assertTrue(triangle is SnappedShape.Triangle)
        assertEquals(Offset(200f, 220f), triangle.p1) // (200, 300 - 80)
        assertEquals(Offset(260f, 380f), triangle.p2) // (200 + 60, 300 + 80)
        assertEquals(Offset(140f, 380f), triangle.p3) // (200 - 60, 300 + 80)
    }

    @Test
    fun testCanvasShapeModelConversion() {
        val modelShape = CanvasShape(
            id = "shape_123",
            type = "RECTANGLE",
            x = 150f,
            y = 200f,
            width = 100f,
            height = 80f,
            colorHex = "#E11D48",
            strokeWidth = 4f,
            lineStyle = "DASHED"
        )

        val snapped = ShapeRecognizer.canvasShapeToSnapped(modelShape)
        assertTrue(snapped is SnappedShape.Rectangle)
        assertEquals(100f, snapped.bounds.left) // 150 - 50
        assertEquals(200f, snapped.bounds.right) // 150 + 50
        assertEquals(160f, snapped.bounds.top) // 200 - 40
        assertEquals(240f, snapped.bounds.bottom) // 200 + 40
    }

    @Test
    fun testShapeManipulationOperations() {
        val initial = CanvasShape(
            id = "shape_test",
            type = "CIRCLE",
            x = 100f,
            y = 100f,
            width = 80f,
            height = 80f,
            colorHex = "#4F46E5",
            strokeWidth = 3f,
            lineStyle = "SOLID"
        )

        // Move
        val moved = initial.copy(x = initial.x + 25f, y = initial.y + 35f)
        assertEquals(125f, moved.x)
        assertEquals(135f, moved.y)

        // Resize
        val resized = moved.copy(width = 120f, height = 120f)
        assertEquals(120f, resized.width)
        assertEquals(120f, resized.height)

        // Change color
        val recolored = resized.copy(colorHex = "#059669")
        assertEquals("#059669", recolored.colorHex)

        // Toggle line style
        val dashed = recolored.copy(lineStyle = "DASHED")
        assertEquals("DASHED", dashed.lineStyle)
    }

    @Test
    fun testCanvasTextBoxManipulation() {
        val initial = CanvasTextBox(
            id = "box_1",
            text = "Handwritten notes meeting minutes",
            x = 50f,
            y = 60f,
            width = 200f,
            height = 90f,
            fontSize = 16f,
            colorHex = "#0F172A"
        )

        // Update text
        val textUpdated = initial.copy(text = "Updated sprint goals")
        assertEquals("Updated sprint goals", textUpdated.text)

        // Move
        val moved = textUpdated.copy(x = 120f, y = 180f)
        assertEquals(120f, moved.x)
        assertEquals(180f, moved.y)

        // Resize
        val resized = moved.copy(width = 260f, height = 110f)
        assertEquals(260f, resized.width)
        assertEquals(110f, resized.height)
    }

    @Test
    fun testCanvasLayerCompoundSerialization() {
        val stroke = InkStroke(
            id = "stroke_dyn_1",
            tool = ToolType.PEN,
            colorHex = "#4F46E5",
            strokeWidth = 3f,
            points = listOf(InkPoint(10f, 20f), InkPoint(30f, 40f))
        )
        val shape = CanvasShape(
            id = "shape_1",
            type = "ELLIPSE",
            x = 200f,
            y = 250f,
            width = 120f,
            height = 80f,
            colorHex = "#E11D48",
            strokeWidth = 4f,
            lineStyle = "DASHED"
        )
        val textBox = CanvasTextBox(
            id = "text_1",
            text = "Architecture Diagram v2",
            x = 80f,
            y = 90f
        )

        val layer = CanvasLayer(
            id = "layer_compound",
            name = "Main Layer",
            strokes = listOf(stroke),
            shapes = listOf(shape),
            textBoxes = listOf(textBox)
        )

        val manifest = CmnManifest(
            noteId = "canvas_100",
            title = "Compound Canvas Note",
            layers = listOf(layer)
        )

        val bytes = CmnPackageSerializer.serialize(manifest)
        assertTrue(CmnPackageSerializer.isValidCmnHeader(bytes))

        val deserialized = CmnPackageSerializer.deserialize(bytes)
        assertEquals(1, deserialized.layers.size)
        val desLayer = deserialized.layers.first()
        assertEquals(1, desLayer.strokes.size)
        assertEquals(1, desLayer.shapes.size)
        assertEquals(1, desLayer.textBoxes.size)

        assertEquals("shape_1", desLayer.shapes.first().id)
        assertEquals("ELLIPSE", desLayer.shapes.first().type)
        assertEquals("DASHED", desLayer.shapes.first().lineStyle)
        assertEquals("Architecture Diagram v2", desLayer.textBoxes.first().text)
    }

    @Test
    fun testSvgExporterWithShapesAndTextBoxes() {
        val shape = CanvasShape(
            id = "shape_rect",
            type = "RECTANGLE",
            x = 100f,
            y = 100f,
            width = 60f,
            height = 40f,
            colorHex = "#4F46E5",
            strokeWidth = 2f,
            lineStyle = "DASHED"
        )
        val textBox = CanvasTextBox(
            id = "text_box_1",
            text = "Important Note Text",
            x = 50f,
            y = 50f,
            fontSize = 14f,
            colorHex = "#000000"
        )
        val layer = CanvasLayer(
            id = "layer_svg",
            name = "Vector Layer",
            shapes = listOf(shape),
            textBoxes = listOf(textBox)
        )

        val svg = SvgExporter.exportLayersToSvg(listOf(layer), "Test Canvas")
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("<rect"))
        assertTrue(svg.contains("stroke-dasharray=\"12,10\""))
        assertTrue(svg.contains("Important Note Text"))
        assertTrue(svg.contains("</svg>"))
    }

    @Test
    fun testCumulativeStrokeAdditionDoesNotOverwritePreviousStrokes() {
        val stroke1 = InkStroke(
            id = "stroke_1",
            tool = ToolType.PEN,
            colorHex = "#0F172A",
            strokeWidth = 3f,
            points = listOf(InkPoint(10f, 10f), InkPoint(20f, 20f))
        )
        val stroke2 = InkStroke(
            id = "stroke_2",
            tool = ToolType.PEN,
            colorHex = "#0F172A",
            strokeWidth = 3f,
            points = listOf(InkPoint(30f, 30f), InkPoint(40f, 40f))
        )

        var layers = listOf(CanvasLayer(id = "layer_1", name = "Test Layer", strokes = listOf(stroke1)))
        assertEquals(1, layers.first().strokes.size)

        // Adding stroke 2 must preserve stroke 1
        layers = layers.mapIndexed { index, layer ->
            if (index == 0) layer.copy(strokes = layer.strokes + stroke2) else layer
        }
        assertEquals(2, layers.first().strokes.size)
        assertEquals("stroke_1", layers.first().strokes[0].id)
        assertEquals("stroke_2", layers.first().strokes[1].id)
    }

    @Test
    fun testEraserIntersectionForStrokesAndShapes() {
        val stroke = InkStroke(
            id = "stroke_target",
            tool = ToolType.PEN,
            colorHex = "#000000",
            strokeWidth = 2f,
            points = listOf(InkPoint(50f, 50f), InkPoint(55f, 55f))
        )
        val distantStroke = InkStroke(
            id = "stroke_safe",
            tool = ToolType.PEN,
            colorHex = "#000000",
            strokeWidth = 2f,
            points = listOf(InkPoint(500f, 500f), InkPoint(510f, 510f))
        )
        val shape = CanvasShape(
            id = "shape_target",
            type = "CIRCLE",
            x = 60f,
            y = 60f,
            width = 30f,
            height = 30f
        )
        val distantShape = CanvasShape(
            id = "shape_safe",
            type = "RECTANGLE",
            x = 400f,
            y = 400f,
            width = 40f,
            height = 40f
        )

        val layer = CanvasLayer(
            id = "test_layer",
            name = "Test Layer",
            strokes = listOf(stroke, distantStroke),
            shapes = listOf(shape, distantShape)
        )

        val eraserPoint = Offset(52f, 52f)
        val eraserRadius = 20f

        val remainingStrokes = layer.strokes.filterNot { s ->
            s.points.any { p ->
                kotlin.math.hypot(eraserPoint.x - p.x, eraserPoint.y - p.y) < eraserRadius
            }
        }
        val remainingShapes = layer.shapes.filterNot { sh ->
            val hw = sh.width / 2f
            val hh = sh.height / 2f
            eraserPoint.x in (sh.x - hw - eraserRadius)..(sh.x + hw + eraserRadius) &&
                    eraserPoint.y in (sh.y - hh - eraserRadius)..(sh.y + hh + eraserRadius)
        }

        assertEquals(1, remainingStrokes.size)
        assertEquals("stroke_safe", remainingStrokes.first().id)

        assertEquals(1, remainingShapes.size)
        assertEquals("shape_safe", remainingShapes.first().id)
    }
}

