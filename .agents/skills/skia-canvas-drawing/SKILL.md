---
name: skia-canvas-drawing
description: >-
  Procedural cheatsheet for high-performance Skia Canvas drawing in Compose Multiplatform.
  Use when developing, optimizing, or debugging handwritten canvas features, ink stroke smoothing
  (Catmull-Rom splines), stylus pressure/tilt sensitivity, drawing instruments (pens, pencils, brushes,
  erasers, highlighters), shape snapping, or canvas layer rendering.
---

# Skia Canvas Drawing Cheatsheet (Compose Multiplatform)

This skill provides step-by-step procedures, mathematical formulas, and idiomatic Kotlin/Compose Multiplatform implementations for building the high-performance Handwritten Notes engine based on the **Samsung Notes** UI/UX reference.

## Architectural Constraints & Scope
- **Core Technology**: Compose Multiplatform `androidx.compose.foundation.Canvas` backed by Skia (`org.jetbrains.skia`).
- **Layout Model**: Vertically continuous scrolling page roll with visual page break dividers.
- **Hardware Support**: Stylus pressure and tilt on supported hardware (Samsung S-Pen, Apple Pencil) with fallback to uniform stroke on touch/mouse.
- **Explicit Constraint**: Programmatic palm rejection is explicitly out of scope and must not be implemented.
- **Layer Stacking**: Multi-layer compositing (Background paper/grid -> Raster images -> Vector ink strokes).

---

## 1. Catmull-Rom Spline Smoothing & Bézier Conversion

To achieve natural, low-latency ink lines without jagged angles, raw sampled touch/stylus points are smoothed via Catmull-Rom splines and converted into cubic Bézier segments for hardware-accelerated Skia path rasterization.

### Conversion Formula
Given four sequential control points $P_0, P_1, P_2, P_3$:
The cubic Bézier control points $C_1, C_2$ between $P_1$ and $P_2$ are derived as:
$$C_1 = P_1 + \frac{P_2 - P_0}{6}$$
$$C_2 = P_2 - \frac{P_3 - P_1}{6}$$

See the detailed mathematical derivation and edge-case handling in:
[Catmull-Rom Spline Reference](./references/catmull_rom_spline.md)

---

## 2. Drawing Instruments & Brush Physics

The canvas engine must support five core writing instruments plus a vector eraser:

| Instrument | Pressure Dynamics | Tilt Dynamics | Alpha / Blend Mode | Cap & Join |
| :--- | :--- | :--- | :--- | :--- |
| **Ballpoint Pen** | Moderate ($\pm 25\%$) | None | Opaque (`SrcOver`) | Round / Round |
| **Fountain Pen** | High dynamic range ($\pm 70\%$) | Velocity modulation | Opaque (`SrcOver`) | Round / Round |
| **Pencil** | Low to moderate | Shading spread on high tilt | Semi-opaque / textured | Round / Round |
| **Calligraphy Brush**| High | Angle-dependent nib width | Opaque (`SrcOver`) | Oval / Bevel |
| **Highlighter** | None (uniform width) | None | 40% opacity (`Multiply` or dedicated under-layer) | Square / Miter |
| **Vector Eraser** | N/A | N/A | Stroke-level hit testing or path subtraction | N/A |

See complete configuration and code samples in:
[Instruments and Brushes Reference](./references/instruments_and_brushes.md)

---

## 3. Shape Recognition & Auto-Snapping

The engine supports two shape creation mechanisms:
1. **Toolbar Shape Tool**: Explicit insertion of standard primitives (Rectangle, Oval/Circle, Line, Wavy Line).
2. **Draw-and-Hold Gesture (0.5s auto-snap)**:
   - When the user draws a stroke and holds the stylus/finger stationary at the end point for $\ge 500\text{ ms}$ with movement $< 5\text{ dp}$.
   - The engine analyzes path closure, aspect ratio, corner count, and linear variance.
   - The raw stroke is replaced by a mathematically snapped geometric shape with smooth transition animation.

See complete detection algorithms in:
[Shape Recognition Reference](./references/shape_recognition.md)

---

## 4. Multi-Layer Canvas Compositing

The canvas maintains a strict layer stack rendered in ascending Z-index order:

1. **Background Layer (Z=0)**:
   - Paper style rendering: Blank, Lined (horizontal rules), Dotted grid, or Quad grid.
   - Page break dividers rendered at fixed A4/custom page height intervals.
2. **Raster Image Layers (Z=100..499)**:
   - Imported gallery images with bounding boxes, rotation, and scaling handles.
3. **Foreground Vector Ink Layers (Z=500..999)**:
   - Vector paths grouped by user-defined layers with independent visibility, locking, and opacity.
4. **Active Stroke Overlay (Top)**:
   - Current in-progress stroke rendered directly without incurring full document recomposition.

See complete implementation in:
[Skia Canvas Sample](./examples/SkiaCanvasSample.kt)

---

## 5. Verification & Testing Procedures

When implementing or modifying canvas code:
1. **Desktop Test**: Run `./gradlew :composeApp:run` to verify mouse and trackpad panning/zooming.
2. **Stylus Pressure Verification**: Verify on Android emulator or S-Pen hardware using `android-cli-plugin` that pressure values range $0.0 \dots 1.0$ and modulate stroke thickness proportionally.
3. **Performance Profiling**: Ensure frame rendering times remain below $16.6\text{ ms}$ (60 FPS) during continuous drawing. Verify paths are cached and not re-evaluated on every frame.
