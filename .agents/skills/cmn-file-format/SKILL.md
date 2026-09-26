---
name: cmn-file-format
description: >-
  Procedural cheatsheet and specification for the .cmn (Custom Multi-layer Note) compound package format.
  Use when creating, parsing, serializing, validating, or migrating .cmn containers, manifest.json schemas,
  vector stroke JSON coordinate arrays, embedded raster images, or exporting canvas notes to SVG.
---

# `.cmn` Compound Package Format Cheatsheet

This skill defines the format specification, data schemas, and serialization pipelines for `.cmn` (Custom Multi-layer Note) packages.

## Architectural Objectives
- **Container Structure**: Bundled ZIP-compatible container with custom header magic bytes (`CMN\x01`) and distinct MIME type `application/x-notes-cmn` (preventing accidental extraction by generic OS archive managers).
- **Hybrid Storage**:
  - Vector data stored as compact JSON coordinate arrays (`points`, `pressure`, `tilt`, `timestamp`) for infinite scaling and non-destructive stroke-level erasing.
  - Raster assets (imported images) preserved losslessly as PNG/JPEG in the `assets/` subfolder.
- **Root Configuration**: `manifest.json` detailing schema version, dimensions, page breaks, and layer hierarchy.
- **Export Compatibility**: Built-in converter to standardized Scalable Vector Graphics (`.svg`).

---

## 1. Container Specification

A `.cmn` file is packaged as follows:

```text
note_sample.cmn
├── mimetype               # Uncompressed "application/x-notes-cmn" (offset 0 if pure zip, or after header)
├── manifest.json          # Root document metadata, dimensions, layer stack
├── layers/                # Vector stroke payloads per layer
│   ├── layer_bg.json
│   ├── layer_ink_1.json
│   └── layer_ink_2.json
├── assets/                # Imported raster images
│   ├── img_001.png
│   └── img_002.jpeg
└── previews/              # Rendered document snapshot
    └── thumbnail.png
```

### Custom Magic Header
To ensure dedicated application ownership:
- Magic bytes: `0x43 0x4D 0x4E 0x01` (`CMN\x01`).
- Appended before the ZIP local file header or stored in the archive comment.

See full binary specification in:
[Container Specification Reference](./references/container_specification.md)

---

## 2. Manifest Schema (`manifest.json`)

The manifest acts as the central index of the compound document:

```json
{
  "schemaVersion": 1,
  "documentId": "c8a14d5e-2f9b-4b2a-8d32-d3a95610bc92",
  "title": "Project Architecture Sketches",
  "createdAt": "2026-09-26T12:00:00Z",
  "updatedAt": "2026-09-26T14:30:00Z",
  "dimensions": {
    "pageWidth": 1200.0,
    "pageHeight": 1600.0,
    "totalRollLength": 4800.0,
    "pageBreakOffsets": [1600.0, 3200.0]
  },
  "background": {
    "style": "GRID",
    "gridSpacing": 40.0,
    "backgroundColorHex": "#FAF9F6",
    "gridLineColorHex": "#E0E0E0"
  },
  "layers": [
    {
      "id": "layer_0_bg",
      "name": "Background",
      "type": "BACKGROUND",
      "zIndex": 0,
      "isVisible": true,
      "isLocked": true,
      "opacity": 1.0
    },
    {
      "id": "layer_1_ink",
      "name": "Ink Layer",
      "type": "VECTOR_INK",
      "dataPath": "layers/layer_ink_1.json",
      "zIndex": 500,
      "isVisible": true,
      "isLocked": false,
      "opacity": 1.0
    }
  ]
}
```

See full schema details in:
[Manifest Schema Reference](./references/manifest_schema.md)

---

## 3. Vector Stroke Serialization & SVG Export

Strokes are serialized into compact coordinate arrays to minimize package footprint and serialization overhead:
`[x, y, pressure, tilt, timestamp]`

### SVG Export Pipeline
Each vector layer is converted into an SVG group (`<g>`):
1. Compute smoothed cubic Bézier spline coordinates via Catmull-Rom interpolation.
2. Generate SVG `<path d="M... C..." stroke="..." stroke-width="..." stroke-linecap="round" fill="none"/>`.
3. Embed raster images via `<image href="data:image/png;base64,..."/>`.

See complete serialization schemas and the SVG converter in:
[Stroke Serialization & SVG Reference](./references/stroke_serialization.md)

---

## 4. Implementation Example

See a production-ready Kotlin Multiplatform package manager with Okio in:
[CMN Package Sample](./examples/CmnPackageSample.kt)
