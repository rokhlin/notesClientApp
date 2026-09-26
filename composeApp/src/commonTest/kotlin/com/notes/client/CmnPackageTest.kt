package com.notes.client

import com.notes.client.canvas.export.CmnPackageSerializer
import com.notes.client.canvas.export.SvgExporter
import com.notes.common.models.*
import kotlin.test.*

class CmnPackageTest {

    private fun sampleManifest(): CmnManifest {
        val stroke1 = InkStroke(
            id = "stroke_1",
            tool = ToolType.PEN,
            colorHex = "#4F46E5",
            strokeWidth = 3f,
            opacity = 1f,
            points = listOf(
                InkPoint(100f, 100f, 0.5f),
                InkPoint(150f, 120f, 0.6f),
                InkPoint(200f, 180f, 0.7f),
                InkPoint(250f, 150f, 0.8f)
            )
        )

        val stroke2 = InkStroke(
            id = "stroke_highlighter",
            tool = ToolType.HIGHLIGHTER,
            colorHex = "#FFFF00",
            strokeWidth = 14f,
            opacity = 0.8f,
            points = listOf(
                InkPoint(80f, 200f, 0.5f),
                InkPoint(300f, 200f, 0.5f)
            )
        )

        val strokeDot = InkStroke(
            id = "stroke_dot",
            tool = ToolType.PEN,
            colorHex = "#FF0000",
            strokeWidth = 4f,
            opacity = 1f,
            points = listOf(
                InkPoint(50f, 50f, 0.5f)
            )
        )

        val layer = CanvasLayer(
            id = "layer_vector_1",
            name = "Main Inking",
            zIndex = 1,
            isVisible = true,
            opacity = 1f,
            layerType = LayerType.VECTOR,
            strokes = listOf(stroke1, stroke2, strokeDot)
        )

        return CmnManifest(
            version = 1,
            noteId = "test_note_123",
            title = "Test Architecture & Sketches <&>",
            layers = listOf(layer),
            createdAt = 1717030000000L,
            updatedAt = 1717030000000L
        )
    }

    @Test
    fun testMagicBytesHeaderPresence() {
        val manifest = sampleManifest()
        val bytes = CmnPackageSerializer.serialize(manifest)

        assertTrue(bytes.size > 4, "Serialized payload must be larger than 4 bytes")
        assertEquals(0x43.toByte(), bytes[0], "Byte 0 must be 'C'")
        assertEquals(0x4D.toByte(), bytes[1], "Byte 1 must be 'M'")
        assertEquals(0x4E.toByte(), bytes[2], "Byte 2 must be 'N'")
        assertEquals(0x01.toByte(), bytes[3], "Byte 3 must be 0x01")
        assertTrue(CmnPackageSerializer.isValidCmnHeader(bytes))
    }

    @Test
    fun testSerializationRoundtripIntegrity() {
        val original = sampleManifest()
        val bytes = CmnPackageSerializer.serialize(original)
        val deserialized = CmnPackageSerializer.deserialize(bytes)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.noteId, deserialized.noteId)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.layers.size, deserialized.layers.size)

        val origLayer = original.layers.first()
        val desLayer = deserialized.layers.first()
        assertEquals(origLayer.id, desLayer.id)
        assertEquals(origLayer.name, desLayer.name)
        assertEquals(origLayer.strokes.size, desLayer.strokes.size)

        val origStroke = origLayer.strokes.first()
        val desStroke = desLayer.strokes.first()
        assertEquals(origStroke.id, desStroke.id)
        assertEquals(origStroke.colorHex, desStroke.colorHex)
        assertEquals(origStroke.strokeWidth, desStroke.strokeWidth)
        assertEquals(origStroke.points.size, desStroke.points.size)
        assertEquals(origStroke.points[0].x, desStroke.points[0].x)
        assertEquals(origStroke.points[0].y, desStroke.points[0].y)
    }

    @Test
    fun testCorruptedMagicHeaderRejection() {
        val original = sampleManifest()
        val bytes = CmnPackageSerializer.serialize(original)

        // Corrupt first byte (e.g. standard PK ZIP header 0x50)
        bytes[0] = 0x50.toByte()

        assertFalse(CmnPackageSerializer.isValidCmnHeader(bytes))
        val exception = assertFailsWith<IllegalArgumentException> {
            CmnPackageSerializer.deserialize(bytes)
        }
        assertTrue(exception.message?.contains("Invalid CMN magic header") == true)
    }

    @Test
    fun testTruncatedPayloadRejection() {
        val truncated = byteArrayOf(0x43, 0x4D)
        assertFalse(CmnPackageSerializer.isValidCmnHeader(truncated))
        assertFailsWith<IllegalArgumentException> {
            CmnPackageSerializer.deserialize(truncated)
        }
    }

    @Test
    fun testSvgExportStructureAndXmlEscape() {
        val manifest = sampleManifest()
        val svg = SvgExporter.exportToSvg(manifest)

        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("<title>Test Architecture &amp; Sketches &lt;&amp;&gt;</title>"))
        assertTrue(svg.contains("<g id=\"layer_vector_1\""))

        // Single point circle check
        assertTrue(svg.contains("<circle cx=\"50\" cy=\"50\""))

        // 2 points line check
        assertTrue(svg.contains("M 80 200 L 300 200"))

        // Highlighter semi-transparency check (0.4 * 0.8 = 0.32)
        assertTrue(svg.contains("opacity=\"0.32\""))

        // Catmull-Rom cubic Bézier path check
        assertTrue(svg.contains("M 100 100 C"))
        assertTrue(svg.contains("stroke=\"#4F46E5\""))
        assertTrue(svg.contains("</svg>"))
    }
}
