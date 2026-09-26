# Drawing Instruments and Brush Configurations

This guide defines the brush characteristics, pressure/tilt sensitivity modulation, and Skia blend modes for the handwritten notes engine based on the **Samsung Notes** UI/UX reference.

## 1. Instrument Specifications

### 1.1 Ballpoint Pen
- **Visual Feel**: Crisp, sharp, reliable ink line.
- **Pressure Sensitivity**: Mild ($w = w_{\text{base}} \times [0.75 + 0.5 \times p]$).
- **Tilt Sensitivity**: None.
- **Cap/Join**: `StrokeCap.Round`, `StrokeJoin.Round`.
- **Alpha**: 1.0 (Fully opaque).
- **Blend Mode**: `BlendMode.SrcOver`.

### 1.2 Fountain Pen
- **Visual Feel**: Dynamic calligraphy feel with responsiveness to writing velocity and pressure.
- **Pressure Sensitivity**: High dynamic range ($w = w_{\text{base}} \times [0.3 + 1.4 \times p^{1.2}]$).
- **Velocity Sensitivity**: Fast movements thin the stroke slightly; slow movements widen and saturate.
- **Cap/Join**: `StrokeCap.Round`, `StrokeJoin.Round`.
- **Alpha**: 1.0.

### 1.3 Pencil
- **Visual Feel**: Granular graphite texture.
- **Pressure Sensitivity**: Modulates both width ($0.8 \dots 1.3$) and opacity ($0.3 \dots 0.85$).
- **Tilt Sensitivity**: High. When stylus tilt exceeds $45^\circ$, the stroke width multiplies by up to $3\times$ while alpha decreases to simulate shading with the side of a pencil lead.
- **Texture**: Shader / dash effect or noise mask applied to Skia paint.

### 1.4 Calligraphy Brush
- **Visual Feel**: Chiseled nib with directional variation.
- **Angle Dynamics**: Fixed 45° virtual nib angle. Strokes moving perpendicular to the nib are thickest, while parallel strokes are thinnest.
- **Pressure**: Increases maximal thickness.
- **Cap/Join**: `StrokeCap.Square`, `StrokeJoin.Bevel`.

### 1.5 Highlighter
- **Visual Feel**: Semi-transparent rectangular marker that highlights text and drawings without obscuring them.
- **Opacity**: 35% – 50% ($0.35f \dots 0.50f$).
- **Cap/Join**: `StrokeCap.Square`, `StrokeJoin.Miter`.
- **Blend Mode**: `BlendMode.Multiply` (or rendered on a dedicated sub-layer beneath ink strokes).
- **Pressure Sensitivity**: None (uniform marker width).

### 1.6 Vector Eraser
- **Stroke Eraser (Default)**: Touch or stylus path intersects with existing vector stroke bounding boxes and line segments. Any intersected stroke is completely removed from the layer.
- **Partial / Area Eraser**: Erases parts of rasterized lines or splits vector strokes into smaller sub-paths using boolean path clipping operations.

---

## 2. Dynamic Modulation Formulas

```kotlin
package com.notes.client.canvas.instruments

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

enum class ToolType {
    BALLPOINT_PEN,
    FOUNTAIN_PEN,
    PENCIL,
    CALLIGRAPHY_BRUSH,
    HIGHLIGHTER,
    VECTOR_ERASER
}

data class BrushConfig(
    val toolType: ToolType,
    val baseWidth: Float = 4f,
    val color: Color = Color.Black,
    val alpha: Float = 1.0f,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val blendMode: BlendMode = BlendMode.SrcOver
) {
    /**
     * Calculates dynamically modulated stroke width for a given pressure and tilt.
     * Pressure ranges from 0.0f to 1.0f.
     * Tilt ranges from 0.0f (perpendicular) to 1.57f (flat).
     */
    fun computeWidth(pressure: Float, tilt: Float): Float {
        val clampedPressure = pressure.coerceIn(0.1f, 1.0f)
        return when (toolType) {
            ToolType.BALLPOINT_PEN -> {
                baseWidth * (0.8f + 0.4f * clampedPressure)
            }
            ToolType.FOUNTAIN_PEN -> {
                baseWidth * (0.3f + 1.4f * kotlin.math.pow(clampedPressure.toDouble(), 1.2).toFloat())
            }
            ToolType.PENCIL -> {
                val tiltFactor = if (tilt > 0.8f) (1f + (tilt - 0.8f) * 2.5f) else 1f
                baseWidth * (0.6f + 0.6f * clampedPressure) * tiltFactor
            }
            ToolType.CALLIGRAPHY_BRUSH -> {
                baseWidth * (0.4f + 1.2f * clampedPressure)
            }
            ToolType.HIGHLIGHTER -> {
                baseWidth // Uniform width
            }
            ToolType.VECTOR_ERASER -> {
                baseWidth * 2.5f
            }
        }
    }
}
```
