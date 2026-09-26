package com.notes.client.storage

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun StorageVaultDialog(
    repository: NoteStorageRepository,
    onDismiss: () -> Unit
) {
    var stats by remember { mutableStateOf(repository.getVaultStats()) }
    var catalog by remember { mutableStateOf(repository.getCatalog()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(620.dp)
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
                            text = "🗄️ Sandboxed Storage & JSON Index",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "ADR Q17 / Q19: Pure Kotlin zero-SQL decoupled file persistence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vault Metrics Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Notes", style = MaterialTheme.typography.labelSmall)
                            Text("${stats.totalNotes}", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Text / Canvas", style = MaterialTheme.typography.labelSmall)
                            Text("${stats.textNotes} / ${stats.canvasNotes}", style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Vault Size", style = MaterialTheme.typography.labelSmall)
                            val sizeKb = (stats.totalSizeBytes / 1024f)
                            val formattedSize = if (sizeKb < 1f) "${stats.totalSizeBytes} B" else "${(sizeKb * 10).toInt() / 10f} KB"
                            Text(formattedSize, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SQL Drivers", style = MaterialTheme.typography.labelSmall)
                            Text("0 (Zero SQL)", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "📄 notes_index.json (Title-Only Metadata Catalog)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Raw Catalog JSON Monospace Viewer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val preview = buildString {
                        appendLine("// Lightweight JSON Index Catalog (ADR Q17: Note bodies strictly excluded)")
                        appendLine("Version: ${catalog.version} | Last Synced: ${catalog.lastSyncedAt}")
                        appendLine("Indexed Notes (${catalog.notes.size}):")
                        catalog.notes.forEachIndexed { i, note ->
                            appendLine("  [${i + 1}] ID: ${note.id} | Title: \"${note.title}\" | Type: ${note.type} | Tags: ${note.tags} | Size: ${note.sizeBytes} B")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = preview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
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
                            val rebuilt = repository.rebuildCatalog()
                            catalog = rebuilt
                            stats = repository.getVaultStats()
                            statusMessage = "Catalog rebuilt successfully! Indexed ${rebuilt.notes.size} note files."
                        }
                    ) {
                        Text("🔄 Rebuild Catalog")
                    }
                }
            }
        }
    }
}
