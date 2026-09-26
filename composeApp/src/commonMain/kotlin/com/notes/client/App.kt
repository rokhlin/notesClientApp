package com.notes.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notes.client.canvas.CanvasToolbar
import com.notes.client.canvas.SkiaHandwrittenCanvas
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.components.ObsidianScaffold
import com.notes.client.components.PrimaryButton
import com.notes.client.editor.MarkdownEngineRegistry
import com.notes.client.editor.WikilinkAutocompletePopup
import com.notes.client.editor.WikilinkParser
import com.notes.client.theme.NotesTheme
import com.notes.common.models.CanvasLayer
import com.notes.common.models.InkPoint
import com.notes.common.models.InkStroke
import com.notes.common.models.LayerType
import com.notes.common.models.Note
import com.notes.common.models.NoteType
import com.notes.common.models.ToolType

@Composable
fun App() {
    var isDarkTheme by remember { mutableStateOf(true) }

    NotesTheme(darkTheme = isDarkTheme) {
        var notes by remember {
            mutableStateOf(
                listOf(
                    Note(
                        id = "1",
                        title = "Architecture Blueprint",
                        content = "# Architecture Blueprint\n\nWelcome to NotesAlltogether unified workspace. Built with Kotlin Multiplatform, Compose Multiplatform, and Obsidian-style ergonomics.\n\nSee [[Getting Started with KMP]] and [[Canvas Wireframes]] for subsystem details.",
                        type = NoteType.TEXT,
                        tags = listOf("architecture", "sdm", "starred"),
                        createdAt = 1717000000000L
                    ),
                    Note(
                        id = "2",
                        title = "Getting Started with KMP",
                        content = "# Getting Started with KMP\n\nCompose Multiplatform shares the UI code across Android, iOS, Desktop, and Web while keeping 100% native performance.\n\nFollows [[Architecture Blueprint]] system boundaries.",
                        type = NoteType.TEXT,
                        tags = listOf("kmp", "compose"),
                        createdAt = 1717010000000L
                    ),
                    Note(
                        id = "3",
                        title = "Canvas Wireframes",
                        content = "# Canvas Wireframes\n\nContinuous vertical roll canvas engine with Catmull-Rom splines, S-Pen tilt/pressure, and .cmn compound storage.\n\nIntegrated with [[Architecture Blueprint]].",
                        type = NoteType.CANVAS,
                        tags = listOf("canvas", "skia", "starred"),
                        createdAt = 1717020000000L
                    )
                )
            )
        }

        var activeNote by remember { mutableStateOf<Note?>(notes.firstOrNull()) }
        var showAddDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var activeEngineId by remember { mutableStateOf("ast-renderer") }
        var isEditMode by remember { mutableStateOf(false) }
        var unresolvedLinkTarget by remember { mutableStateOf<String?>(null) }
        val currentEngine = remember(activeEngineId) { MarkdownEngineRegistry.getEngine(activeEngineId) }

        // Canvas state for handwritten notes
        var canvasBrush by remember { mutableStateOf(BrushConfig.defaultFor(ToolType.PEN)) }
        var canvasLayersByNoteId by remember {
            mutableStateOf<Map<String, List<CanvasLayer>>>(
                mapOf(
                    "3" to listOf(
                        CanvasLayer(
                            id = "layer_3_1",
                            name = "Vector Layer 1",
                            layerType = LayerType.VECTOR,
                            strokes = listOf(
                                InkStroke(
                                    id = "initial_stroke",
                                    tool = ToolType.PEN,
                                    colorHex = "#4F46E5",
                                    strokeWidth = 4f,
                                    points = listOf(
                                        InkPoint(100f, 150f, 0.5f),
                                        InkPoint(200f, 130f, 0.7f),
                                        InkPoint(300f, 220f, 0.9f),
                                        InkPoint(400f, 180f, 0.6f)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
        var canvasUndoHistory by remember { mutableStateOf<List<List<CanvasLayer>>>(emptyList()) }
        var canvasRedoHistory by remember { mutableStateOf<List<List<CanvasLayer>>>(emptyList()) }

        // Compute incoming backlinks dynamically for the active note
        val activeBacklinks = remember(activeNote, notes) {
            activeNote?.let {
                WikilinkParser.findBacklinks(it.title, notes)
            } ?: emptyList()
        }

        ObsidianScaffold(
            notes = notes,
            activeNote = activeNote,
            isDarkTheme = isDarkTheme,
            activeEngineId = activeEngineId,
            onEngineSelected = { activeEngineId = it },
            backlinks = activeBacklinks,
            onBacklinkClick = { sourceId ->
                notes.find { it.id == sourceId }?.let { activeNote = it }
            },
            onNoteSelected = { note ->
                activeNote = note
            },
            onCreateNote = { title ->
                val newNote = Note(
                    id = (notes.size + 1).toString(),
                    title = title.ifBlank { "Untitled Note" },
                    content = "# ${title.ifBlank { "Untitled Note" }}\n\n",
                    type = NoteType.TEXT,
                    tags = listOf("new"),
                    createdAt = 1717030000000L
                )
                notes = listOf(newNote) + notes
                activeNote = newNote
            },
            onToggleTheme = { isDarkTheme = !isDarkTheme },
            onOpenSettings = { showSettingsDialog = true }
        ) { currentNote ->
            if (currentNote != null) {
                // Unified Workspace Canvas (NO TABS) - Single Document Focus
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    if (currentNote.type == NoteType.CANVAS) {
                        val currentLayers = canvasLayersByNoteId[currentNote.id] ?: listOf(
                            CanvasLayer(
                                id = "layer_${currentNote.id}",
                                name = "Vector Layer 1",
                                layerType = LayerType.VECTOR,
                                strokes = emptyList()
                            )
                        )

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Bar with Canvas Note Title & Details
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentNote.title,
                                    onValueChange = { newTitle ->
                                        val updated = currentNote.copy(title = newTitle)
                                        activeNote = updated
                                        notes = notes.map { if (it.id == updated.id) updated else it }
                                    },
                                    label = { Text("Canvas Document Title") },
                                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                                    singleLine = true
                                )
                                AssistChip(
                                    onClick = { },
                                    label = { Text("✏️ Skia Continuous Roll") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            // Interactive Viewport with Skia Handwritten Canvas and Floating Samsung Notes Toolbar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                SkiaHandwrittenCanvas(
                                    modifier = Modifier.fillMaxSize(),
                                    layers = currentLayers,
                                    onLayersChange = { newLayers ->
                                        canvasUndoHistory = canvasUndoHistory + listOf(currentLayers)
                                        canvasRedoHistory = emptyList()
                                        canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to newLayers)
                                    },
                                    currentBrush = canvasBrush
                                )

                                CanvasToolbar(
                                    currentBrush = canvasBrush,
                                    onBrushChange = { canvasBrush = it },
                                    canUndo = canvasUndoHistory.isNotEmpty(),
                                    canRedo = canvasRedoHistory.isNotEmpty(),
                                    onUndo = {
                                        if (canvasUndoHistory.isNotEmpty()) {
                                            val previousState = canvasUndoHistory.last()
                                            canvasUndoHistory = canvasUndoHistory.dropLast(1)
                                            canvasRedoHistory = canvasRedoHistory + listOf(currentLayers)
                                            canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to previousState)
                                        }
                                    },
                                    onRedo = {
                                        if (canvasRedoHistory.isNotEmpty()) {
                                            val nextState = canvasRedoHistory.last()
                                            canvasRedoHistory = canvasRedoHistory.dropLast(1)
                                            canvasUndoHistory = canvasUndoHistory + listOf(currentLayers)
                                            canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to nextState)
                                        }
                                    },
                                    onClear = {
                                        canvasUndoHistory = canvasUndoHistory + listOf(currentLayers)
                                        canvasRedoHistory = emptyList()
                                        val cleared = currentLayers.map { it.copy(strokes = emptyList()) }
                                        canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to cleared)
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    } else {
                        // Markdown Text Editor Canvas with Pluggable Engine & Wikilinks
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentNote.title,
                                onValueChange = { newTitle ->
                                    val updated = currentNote.copy(title = newTitle)
                                    activeNote = updated
                                    notes = notes.map { if (it.id == updated.id) updated else it }
                                },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )

                            // Edit / Preview toggle pill
                            TextButton(
                                onClick = { isEditMode = !isEditMode }
                            ) {
                                Text(
                                    text = if (isEditMode) "👁️ View (${currentEngine.displayName})" else "✏️ Edit Source",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isEditMode) {
                            val content = currentNote.content
                            // Check if cursor/content currently has an active [[ autocomplete query
                            val showAutocomplete = content.contains("[[") && !content.substringAfterLast("[[").contains("]")
                            val autocompleteQuery = if (showAutocomplete) content.substringAfterLast("[[").trim() else ""

                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                OutlinedTextField(
                                    value = currentNote.content,
                                    onValueChange = { newContent ->
                                        val updated = currentNote.copy(content = newContent)
                                        activeNote = updated
                                        notes = notes.map { if (it.id == updated.id) updated else it }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxSize(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )

                                if (showAutocomplete) {
                                    WikilinkAutocompletePopup(
                                        query = autocompleteQuery,
                                        allNotes = notes,
                                        onSelectNote = { selected ->
                                            val prefix = content.substringBeforeLast("[[")
                                            val newContent = "$prefix[[${selected.title}]] "
                                            val updated = currentNote.copy(content = newContent)
                                            activeNote = updated
                                            notes = notes.map { if (it.id == updated.id) updated else it }
                                        },
                                        onDismiss = { /* Dismiss popup */ },
                                        modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp)
                                    )
                                }
                            }
                        } else {
                            // Pluggable Engine Rendering with Wikilink Navigation
                            currentEngine.Render(
                                content = currentNote.content,
                                modifier = Modifier.weight(1f),
                                onLinkClick = { linkTitle ->
                                    val target = notes.find { it.title.equals(linkTitle, ignoreCase = true) }
                                    if (target != null) {
                                        activeNote = target
                                    } else {
                                        unresolvedLinkTarget = linkTitle
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Empty State / Welcome Screen
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚡ Welcome to NotesAlltogether",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No note selected. Select a note from the left sidebar or press Ctrl/Cmd+O to search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = "＋ Create Note",
                            onClick = { showAddDialog = true }
                        )
                    }
                }
            }
        }

        // Unresolved Note Creation Dialog
        unresolvedLinkTarget?.let { targetTitle ->
            AlertDialog(
                onDismissRequest = { unresolvedLinkTarget = null },
                title = { Text("Create Linked Note") },
                text = {
                    Text("The note \"$targetTitle\" does not exist yet. Would you like to create and open it now?")
                },
                confirmButton = {
                    PrimaryButton(
                        text = "Create Note",
                        onClick = {
                            val newNote = Note(
                                id = (notes.size + 1).toString(),
                                title = targetTitle,
                                content = "# $targetTitle\n\n",
                                type = NoteType.TEXT,
                                tags = listOf("linked"),
                                createdAt = 1717040000000L
                            )
                            notes = listOf(newNote) + notes
                            activeNote = newNote
                            unresolvedLinkTarget = null
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { unresolvedLinkTarget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddDialog) {
            AddNoteDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, content ->
                    val newNote = Note(
                        id = (notes.size + 1).toString(),
                        title = title,
                        content = content,
                        createdAt = 1717030000000L
                    )
                    notes = listOf(newNote) + notes
                    activeNote = newNote
                    showAddDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings & Vault") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Theme", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { isDarkTheme = it }
                            )
                        }
                        Text(
                            "NotesAlltogether v1.0.0 (Compose Multiplatform)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Create",
                enabled = title.isNotBlank(),
                onClick = { onAdd(title, content) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
