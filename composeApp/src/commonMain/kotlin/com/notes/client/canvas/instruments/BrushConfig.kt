package com.notes.client.canvas.instruments

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.notes.common.models.ToolType
import kotlin.math.pow

data class BrushConfig(
    val toolType: ToolType = ToolType.PEN,
    val baseWidth: Float = 4f,
    val color: Color = Color(0xFF0F172A),
    val alpha: Float = 1.0f,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val blendMode: BlendMode = BlendMode.SrcOver
) {
    /**
     * Calculates modulated stroke width based on tool physics and stylus pressure/tilt.
     */
    fun calculateEffectiveWidth(pressure: Float = 0.5f, tilt: Float = 0f): Float {
        val p = pressure.coerceIn(0.1f, 1.0f)
        return when (toolType) {
            ToolType.PEN -> baseWidth * (0.75f + 0.5f * p)
            ToolType.FOUNTAIN_PEN -> baseWidth * (0.3f + 1.4f * p.pow(1.2f))
            ToolType.PENCIL -> {
                val tiltFactor = if (tilt > 0.785f) 2.0f else 1.0f // > 45 degrees
                baseWidth * (0.8f + 0.5f * p) * tiltFactor
            }
            ToolType.CALLIGRAPHY_BRUSH -> baseWidth * (0.5f + 1.2f * p)
            ToolType.HIGHLIGHTER -> baseWidth * 3.5f // Uniform marker width
            ToolType.VECTOR_ERASER -> baseWidth * 3.0f
        }
    }

    companion object {
        fun defaultFor(tool: ToolType, color: Color = Color(0xFF0F172A), baseWidth: Float = 4f): BrushConfig {
            return when (tool) {
                ToolType.PEN -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = color,
                    alpha = 1.0f,
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = BlendMode.SrcOver
                )
                ToolType.FOUNTAIN_PEN -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = color,
                    alpha = 1.0f,
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = BlendMode.SrcOver
                )
                ToolType.PENCIL -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = color,
                    alpha = 0.65f,
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = BlendMode.SrcOver
                )
                ToolType.CALLIGRAPHY_BRUSH -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = color,
                    alpha = 1.0f,
                    strokeCap = StrokeCap.Square,
                    strokeJoin = StrokeJoin.Bevel,
                    blendMode = BlendMode.SrcOver
                )
                ToolType.HIGHLIGHTER -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = color,
                    alpha = 0.35f,
                    strokeCap = StrokeCap.Square,
                    strokeJoin = StrokeJoin.Miter,
                    blendMode = BlendMode.SrcOver
                )
                ToolType.VECTOR_ERASER -> BrushConfig(
                    toolType = tool,
                    baseWidth = baseWidth,
                    color = Color.Transparent,
                    alpha = 1.0f,
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
}
