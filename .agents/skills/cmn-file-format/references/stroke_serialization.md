# Vector Stroke Serialization and SVG Vector Export

This document details the serialization of stroke data into compact JSON coordinate arrays and the vector export pipeline to SVG.

## 1. Stroke Layer Serialization Format (`layers/layer_ink_*.json`)

To minimize file size and avoid JSON property repetition for every touch point, coordinates are stored as compact 5-element numeric tuples:
`[x, y, pressure, tilt, timestamp_offset_ms]`

### JSON Format Example
```json
{
  "layerId": "layer_1_ink",
  "strokes": [
    {
      "id": "stk_01H12345",
      "tool": "FOUNTAIN_PEN",
      "color": "#FF1E1E1E",
      "baseWidth": 4.5,
      "blendMode": "SrcOver",
      "points": [
        [104.2, 230.5, 0.45, 0.0, 0],
        [108.7, 234.1, 0.62, 0.0, 16],
        [115.3, 241.0, 0.78, 0.1, 32],
        [122.9, 248.8, 0.85, 0.1, 48]
      ]
    }
  ],
  "shapes": [
    {
      "id": "shp_01H12346",
      "type": "RECTANGLE",
      "color": "#FF2979FF",
      "strokeWidth": 3.0,
      "bounds": [150.0, 300.0, 450.0, 500.0],
      "isFilled": false
    }
  ]
}
```

---

## 2. Kotlin Serialization Model

```kotlin
package com.notes.common.models.cmn

import kotlinx.serialization.Serializable

@Serializable
data class CmnLayerData(
    val layerId: String,
    val strokes: List<CmnStroke> = emptyList(),
    val shapes: List<CmnShape> = emptyList(),
    val images: List<CmnImagePlacement> = emptyList()
)

@Serializable
data class CmnStroke(
    val id: String,
    val tool: String,
    val color: String, // Hex "#AARRGGBB"
    val baseWidth: Float,
    val blendMode: String = "SrcOver",
    // Points stored as [x, y, pressure, tilt, timestampOffsetMs]
    val points: List<List<Float>>
)

@Serializable
data class CmnShape(
    val id: String,
    val type: String, // RECTANGLE, CIRCLE, ELLIPSE, LINE
    val color: String,
    val strokeWidth: Float,
    val bounds: List<Float>, // [minX, minY, maxX, maxY]
    val isFilled: Boolean = false,
    val fillColor: String? = null
)

@Serializable
data class CmnImagePlacement(
    val id: String,
    val assetPath: String, // e.g. "assets/img_001.png"
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float = 0f
)
```

---

## 3. SVG Vector Exporter

Exporting a `.cmn` note to standardized SVG requires converting Catmull-Rom smoothed Bézier paths into SVG `<path d="...">` strings.

```kotlin
package com.notes.common.export.svg

import com.notes.common.models.cmn.CmnManifest
import com.notes.common.models.cmn.CmnLayerData
import com.notes.common.models.cmn.CmnStroke

object SvgExporter {

    fun exportToSvg(
        manifest: CmnManifest,
        layers: List<CmnLayerData>
    ): String {
        val width = manifest.dimensions.pageWidth
        val height = manifest.dimensions.totalRollLength

        val sb = StringBuilder()
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="$width" height="$height">""")
        
        // 1. Background Rect
        sb.appendLine("""  <rect width="100%" height="100%" fill="${manifest.background.backgroundColorHex}"/>""")

        // 2. Layer Groups
        layers.forEach { layer ->
            sb.appendLine("""  <g id="${layer.layerId}">""")
            layer.strokes.forEach { stroke ->
                val pathData = strokePointsToSvgPath(stroke.points)
                val strokeColor = hexToRgba(stroke.color)
                val opacity = if (stroke.tool == "HIGHLIGHTER") 0.4f else 1.0f

                sb.appendLine(
                    """    <path d="$pathData" stroke="$strokeColor" stroke-width="${stroke.baseWidth}" stroke-linecap="round" stroke-linejoin="round" fill="none" opacity="$opacity"/>"""
                )
            }
            sb.appendLine("""  </g>""")
        }

        sb.appendLine("""</svg>""")
        return sb.toString()
    }

    private fun strokePointsToSvgPath(rawPoints: List<List<Float>>): String {
        if (rawPoints.isEmpty()) return ""
        if (rawPoints.size == 1) {
            val x = rawPoints[0][0]
            val y = rawPoints[0][1]
            return "M $x $y A 0.5 0.5 0 0 1 $x ${y + 0.1}"
        }

        val sb = StringBuilder()
        sb.append("M ${rawPoints[0][0]} ${rawPoints[0][1]}")

        for (i in 0 until rawPoints.size - 1) {
            val p0 = if (i == 0) rawPoints[0] else rawPoints[i - 1]
            val p1 = rawPoints[i]
            val p2 = rawPoints[i + 1]
            val p3 = if (i + 2 < rawPoints.size) rawPoints[i + 2] else rawPoints[i + 1]

            val c1x = p1[0] + (p2[0] - p0[0]) / 6f
            val c1y = p1[1] + (p2[1] - p0[1]) / 6f
            val c2x = p2[0] - (p3[0] - p1[0]) / 6f
            val c2y = p2[1] - (p3[1] - p1[1]) / 6f

            sb.append(" C $c1x $c1y, $c2x $c2y, ${p2[0]} ${p2[1]}")
        }

        return sb.toString()
    }

    private fun hexToRgba(hex: String): String {
        return if (hex.startsWith("#") && hex.length == 9) {
            // #AARRGGBB to #RRGGBB
            "#" + hex.substring(3)
        } else {
            hex
        }
    }
}
```
