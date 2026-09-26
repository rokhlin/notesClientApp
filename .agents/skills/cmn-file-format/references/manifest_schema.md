# Manifest Schema Specification (`manifest.json`)

The `manifest.json` file resides at the root of every `.cmn` container and provides metadata, coordinate dimensions, page divider offsets, and the layer compositing stack.

## 1. JSON Schema Definition

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "CmnManifest",
  "type": "object",
  "required": [
    "schemaVersion",
    "documentId",
    "title",
    "createdAt",
    "updatedAt",
    "dimensions",
    "layers"
  ],
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "minimum": 1
    },
    "documentId": {
      "type": "string",
      "format": "uuid"
    },
    "title": {
      "type": "string"
    },
    "createdAt": {
      "type": "string",
      "format": "date-time"
    },
    "updatedAt": {
      "type": "string",
      "format": "date-time"
    },
    "dimensions": {
      "type": "object",
      "required": ["pageWidth", "pageHeight", "totalRollLength"],
      "properties": {
        "pageWidth": { "type": "number" },
        "pageHeight": { "type": "number" },
        "totalRollLength": { "type": "number" },
        "pageBreakOffsets": {
          "type": "array",
          "items": { "type": "number" }
        }
      }
    },
    "background": {
      "type": "object",
      "required": ["style", "backgroundColorHex"],
      "properties": {
        "style": {
          "type": "string",
          "enum": ["BLANK", "LINED", "DOTTED", "GRID"]
        },
        "gridSpacing": { "type": "number" },
        "backgroundColorHex": { "type": "string" },
        "gridLineColorHex": { "type": "string" }
      }
    },
    "layers": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["id", "name", "type", "zIndex", "isVisible", "isLocked", "opacity"],
        "properties": {
          "id": { "type": "string" },
          "name": { "type": "string" },
          "type": {
            "type": "string",
            "enum": ["BACKGROUND", "VECTOR_INK", "RASTER_IMAGE"]
          },
          "dataPath": { "type": "string" },
          "zIndex": { "type": "integer" },
          "isVisible": { "type": "boolean" },
          "isLocked": { "type": "boolean" },
          "opacity": {
            "type": "number",
            "minimum": 0.0,
            "maximum": 1.0
          },
          "blendMode": {
            "type": "string",
            "default": "SrcOver"
          }
        }
      }
    }
  }
}
```

## 2. Kotlin Data Models (`kotlinx.serialization`)

```kotlin
package com.notes.common.models.cmn

import kotlinx.serialization.Serializable

@Serializable
data class CmnManifest(
    val schemaVersion: Int = 1,
    val documentId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val dimensions: CmnDimensions,
    val background: CmnBackground = CmnBackground(),
    val layers: List<CmnLayerMetadata> = emptyList()
)

@Serializable
data class CmnDimensions(
    val pageWidth: Float = 1200f,
    val pageHeight: Float = 1600f,
    val totalRollLength: Float = 1600f,
    val pageBreakOffsets: List<Float> = emptyList()
)

@Serializable
data class CmnBackground(
    val style: String = "BLANK", // BLANK, LINED, DOTTED, GRID
    val gridSpacing: Float = 40f,
    val backgroundColorHex: String = "#FAF9F6",
    val gridLineColorHex: String = "#E0E0E0"
)

@Serializable
data class CmnLayerMetadata(
    val id: String,
    val name: String,
    val type: String, // BACKGROUND, VECTOR_INK, RASTER_IMAGE
    val dataPath: String? = null,
    val zIndex: Int = 0,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val blendMode: String = "SrcOver"
)
```
