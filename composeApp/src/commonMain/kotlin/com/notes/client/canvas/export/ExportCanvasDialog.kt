package com.notes.client.canvas.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.notes.common.models.CanvasLayer
import com.notes.common.models.CmnManifest

@Composable
fun ExportCanvasDialog(
    title: String,
    layers: List<CanvasLayer>,
    onDismiss: () -> Unit
) {
    var selectedFormatIndex by remember { mutableStateOf(0) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val manifest = remember(title, layers) {
        CmnManifest(
            version = 1,
            noteId = "canvas_export_${title.hashCode()}",
            title = title.ifBlank { "Untitled Canvas" },
            layers = layers,
            createdAt = 1717030000000L,
            updatedAt = 1717030000000L
        )
    }

    val totalStrokes = remember(layers) {
        layers.sumOf { it.strokes.size }
    }

    val svgContent = remember(manifest) {
        SvgExporter.exportToSvg(manifest)
    }

    val cmnBytes = remember(manifest) {
        CmnPackageSerializer.serialize(manifest)
    }

    val isHeaderValid = remember(cmnBytes) {
        CmnPackageSerializer.isValidCmnHeader(cmnBytes)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(640.dp)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📤 Export Handwritten Note",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Export as Scalable Vector Graphics or Compound .cmn Package",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format Selector Tabs
                TabRow(
                    selectedTabIndex = selectedFormatIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedFormatIndex == 0,
                        onClick = {
                            selectedFormatIndex = 0
                            exportStatusMessage = null
                        },
                        text = { Text("W3C SVG Vector (.svg)") }
                    )
                    Tab(
                        selected = selectedFormatIndex == 1,
                        onClick = {
                            selectedFormatIndex = 1
                            exportStatusMessage = null
                        },
                        text = { Text("Compound Package (.cmn)") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics / Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Layers", style = MaterialTheme.typography.labelSmall)
                            Text("${layers.size}", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Strokes", style = MaterialTheme.typography.labelSmall)
                            Text("$totalStrokes", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Size", style = MaterialTheme.typography.labelSmall)
                            val sizeText = if (selectedFormatIndex == 0) {
                                "${svgContent.encodeToByteArray().size} B"
                            } else {
                                "${cmnBytes.size} B"
                            }
                            Text(sizeText, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Magic Header", style = MaterialTheme.typography.labelSmall)
                            Text(
                                if (isHeaderValid) "CMN\\x01 Valid" else "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isHeaderValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val previewText = if (selectedFormatIndex == 0) {
                        svgContent
                    } else {
                        "// CMN\\x01 Package Binary Header [0x43, 0x4D, 0x4E, 0x01]\n" +
                                "// Encoded JSON Manifest Payload (${cmnBytes.size} bytes):\n\n" +
                                cmnBytes.copyOfRange(4, cmnBytes.size).decodeToString()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                exportStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val textToCopy = if (selectedFormatIndex == 0) {
                                svgContent
                            } else {
                                cmnBytes.copyOfRange(4, cmnBytes.size).decodeToString()
                            }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            exportStatusMessage = "Copied to clipboard!"
                        }
                    ) {
                        Text("📋 Copy Code")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            exportStatusMessage = if (selectedFormatIndex == 0) {
                                "Exported ${title.ifBlank { "note" }}.svg (${svgContent.length} chars) successfully!"
                            } else {
                                "Saved ${title.ifBlank { "note" }}.cmn (${cmnBytes.size} bytes) with valid magic header!"
                            }
                        }
                    ) {
                        Text(if (selectedFormatIndex == 0) "💾 Save SVG" else "💾 Save .cmn")
                    }
                }
            }
        }
    }
}
