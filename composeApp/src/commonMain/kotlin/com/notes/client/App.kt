package com.notes.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.notes.client.canvas.export.ExportCanvasDialog
import com.notes.client.canvas.instruments.BrushConfig
import com.notes.client.canvas.shapes.ShapeRecognizer
import com.notes.client.components.ObsidianScaffold
import com.notes.client.components.PrimaryButton
import com.notes.client.crypto.VaultUnlockDialog
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.notes.client.editor.EditorToolbar
import com.notes.client.editor.MarkdownEngineRegistry
import com.notes.client.editor.WikilinkAutocompletePopup
import com.notes.client.editor.WikilinkParser
import com.notes.client.storage.JsonIndexNoteRepository
import com.notes.client.storage.StorageVaultDialog
import com.notes.client.theme.NotesTheme
import com.notes.client.auth.AuthManager
import com.notes.client.auth.LoginRequiredDialog
import com.notes.client.components.SettingsDialog
import com.notes.client.components.SettingsScreen
import com.notes.client.components.SetPasswordProtectionDialog
import com.notes.client.components.RemovePasswordProtectionDialog
import com.notes.client.components.NoteSharingDialog
import com.notes.client.crypto.ProtectedNoteBarrier
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.client.util.currentTimeMillis
import androidx.compose.ui.unit.sp
import com.notes.common.crypto.ProtectedNoteCodec
import com.notes.client.ai.AiClientService
import com.notes.client.ai.DefaultAiClientService
import com.notes.client.components.SmartMetadataDialog
import com.notes.common.models.AiMetadataRequest
import com.notes.common.models.AiProviderConfig
import com.notes.common.models.AiProviderType
import com.notes.common.models.CanvasLayer
import com.notes.common.models.InkPoint
import com.notes.common.models.InkStroke
import com.notes.common.models.LayerType
import com.notes.common.models.Note
import com.notes.common.models.NoteMetadataFill
import com.notes.common.models.NoteType
import com.notes.common.models.ToolType
import com.notes.common.models.CanvasShape
import com.notes.common.models.CanvasTextBox
import com.notes.client.canvas.shapes.RecognizedShapeType
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun App() {
    val deviceSettingsDriver = remember { DeviceSettingsDriver() }
    var generalSettings by remember { mutableStateOf(deviceSettingsDriver.getGeneralSettings()) }
    var isDarkTheme by remember {
        mutableStateOf(
            when (generalSettings.theme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> true
            }
        )
    }
    var editorFontSize by remember { mutableStateOf(generalSettings.editorFontSize) }
    var toolbarConfig by remember { mutableStateOf(deviceSettingsDriver.getToolbarConfig()) }

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
                    ),
                    Note(
                        id = "4",
                        title = "Security & Storage Manifest",
                        content = ProtectedNoteCodec.pack(
                            ProtectedNoteCodec.createProtectedNote(
                                noteId = "4",
                                title = "Security & Storage Manifest",
                                password = "secret123",
                                payloadContent = "# Security & Storage Manifest\n\n- Individual Note Password Protection (Self-Contained .nap Container)\n- Unified R2 Cloud Storage with per-tenant users/{userId}/ isolation\n- API request authentication with userApiKey + HMAC-SHA256 signature\n- Two-tier configuration: UserCloudConfig (synced) & DeviceLocalModuleConfig (hardware)",
                                passwordHint = "Default demo password: 'secret123'"
                            )
                        ),
                        type = NoteType.TEXT,
                        tags = listOf("security", "protected", "starred"),
                        isProtected = true,
                        createdAt = 1717030000000L
                    ),
                    Note(
                        id = "5",
                        title = "E2EE Master Key Vault Spec",
                        content = "# E2EE Master Key Vault Spec\n\n- Zero-Knowledge client-side authenticated encryption (AES-GCM-256)\n- Non-custodial 12-word BIP-39 recovery mnemonic seed phrase\n- Constant-time MAC authentication tag verification",
                        type = NoteType.TEXT,
                        tags = listOf("security", "crypto"),
                        isEncrypted = true,
                        createdAt = 1717035000000L
                    )
                )
            )
        }

        val authManager = remember { AuthManager() }
        var showSettingsScreen by remember { mutableStateOf(false) }
        var showProtectDialog by remember { mutableStateOf(false) }
        var showUnprotectDialog by remember { mutableStateOf(false) }
        var showShareDialog by remember { mutableStateOf(false) }
        var showLoginRequiredDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var unlockedNoteIds by remember { mutableStateOf(setOf<String>()) }

        var activeNote by remember { mutableStateOf<Note?>(notes.firstOrNull()) }
        var showAddDialog by remember { mutableStateOf(false) }
        var activeEngineId by remember { mutableStateOf("ast-renderer") }
        var isEditMode by remember { mutableStateOf(false) }
        var unresolvedLinkTarget by remember { mutableStateOf<String?>(null) }
        val currentEngine = remember(activeEngineId) { MarkdownEngineRegistry.getEngine(activeEngineId) }

        // Canvas state for handwritten notes
        var canvasBrush by remember { mutableStateOf(BrushConfig.defaultFor(ToolType.PEN)) }
        var pendingShapeType by remember { mutableStateOf<RecognizedShapeType?>(null) }
        var isCanvasToolbarCollapsed by remember { mutableStateOf(false) }
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
        var showExportCanvasDialog by remember { mutableStateOf(false) }
        var showStorageDialog by remember { mutableStateOf(false) }
        var isVaultUnlocked by remember { mutableStateOf(false) }
        var showUnlockVaultDialog by remember { mutableStateOf(false) }

        val storageRepository = remember {
            val repo = JsonIndexNoteRepository()
            if (repo.getCatalog().notes.isEmpty()) {
                notes.forEach { repo.saveNote(it) }
            }
            repo
        }

        // Compute incoming backlinks dynamically for the active note
        val activeBacklinks = remember(activeNote, notes) {
            activeNote?.let {
                WikilinkParser.findBacklinks(it.title, notes)
            } ?: emptyList()
        }

        val aiClientService = remember { DefaultAiClientService() }
        val coroutineScope = rememberCoroutineScope()
        var showSmartMetadataDialog by remember { mutableStateOf(false) }
        var activeMetadataFill by remember { mutableStateOf<NoteMetadataFill?>(null) }
        var isAiLoading by remember { mutableStateOf(false) }
        var aiErrorMessage by remember { mutableStateOf<String?>(null) }
        var showAiErrorDialog by remember { mutableStateOf(false) }
        var showPrivacyWarningDialog by remember { mutableStateOf(false) }
        var privacyWarningPendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        fun executeAiMetadataFill(note: Note) {
            val aiSettings = deviceSettingsDriver.getAiSettingsConfig()
            val providerConfig = aiSettings.providers[aiSettings.activeProvider] ?: AiProviderConfig(providerType = aiSettings.activeProvider)
            isAiLoading = true
            coroutineScope.launch {
                val request = AiMetadataRequest(
                    noteId = note.id,
                    title = note.title,
                    content = if (note.isProtected && note.content.startsWith(ProtectedNoteCodec.MAGIC_HEADER)) {
                        runCatching { ProtectedNoteCodec.unpack(note.content).payloadContent }.getOrDefault(note.content)
                    } else {
                        note.content
                    },
                    existingTags = note.tags,
                    maxTags = aiSettings.maxTagsToGenerate
                )
                val result = aiClientService.fillMetadata(request, providerConfig)
                isAiLoading = false
                result.fold(
                    onSuccess = { fill ->
                        activeMetadataFill = fill
                        showSmartMetadataDialog = true
                    },
                    onFailure = { error ->
                        aiErrorMessage = error.message ?: "Failed to generate metadata"
                        showAiErrorDialog = true
                    }
                )
            }
        }

        fun triggerAiMetadata() {
            val note = activeNote ?: return
            if (note.isProtected && !unlockedNoteIds.contains(note.id)) {
                aiErrorMessage = "This note is password-protected. Please unlock it before requesting AI metadata."
                showAiErrorDialog = true
                return
            }
            if (note.isEncrypted && !isVaultUnlocked) {
                aiErrorMessage = "This note is encrypted. Please unlock your vault before requesting AI metadata."
                showAiErrorDialog = true
                return
            }

            val aiSettings = deviceSettingsDriver.getAiSettingsConfig()
            if (aiSettings.activeProvider != AiProviderType.LOCAL_SERVER && (note.isProtected || note.isEncrypted)) {
                privacyWarningPendingAction = { executeAiMetadataFill(note) }
                showPrivacyWarningDialog = true
            } else {
                executeAiMetadataFill(note)
            }
        }

        if (showSettingsScreen) {
            SettingsScreen(
                authManager = authManager,
                deviceSettingsDriver = deviceSettingsDriver,
                onNavigateBack = { showSettingsScreen = false },
                onOpenStorageVault = { showStorageDialog = true },
                onThemeChanged = { themeCode ->
                    isDarkTheme = when (themeCode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> true
                    }
                },
                onFontSizeChanged = { size ->
                    editorFontSize = size
                },
                onToolbarConfigChanged = {
                    toolbarConfig = deviceSettingsDriver.getToolbarConfig()
                }
            )
            return@NotesTheme
        }

        ObsidianScaffold(
            notes = notes,
            activeNote = activeNote,
            isDarkTheme = isDarkTheme,
            isEditMode = isEditMode,
            isNoteUnlocked = activeNote?.let { !it.isProtected || unlockedNoteIds.contains(it.id) } ?: true,
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
                    createdAt = currentTimeMillis()
                )
                storageRepository.saveNote(newNote)
                notes = listOf(newNote) + notes
                activeNote = newNote
            },
            onToggleTheme = { isDarkTheme = !isDarkTheme },
            onOpenSettings = {
                showSettingsScreen = true
            },
            onTriggerAiMetadata = { triggerAiMetadata() },
            onToggleEditMode = { isEditMode = !isEditMode },
            onRenameNote = { newTitle ->
                activeNote?.let { cur ->
                    val updated = cur.copy(title = newTitle, updatedAt = currentTimeMillis())
                    activeNote = updated
                    notes = notes.map { if (it.id == updated.id) updated else it }
                    storageRepository.saveNote(updated)
                }
            },
            onProtectNote = { showProtectDialog = true },
            onUnprotectNote = { showUnprotectDialog = true },
            onShareNote = { showShareDialog = true }
        ) { currentNote ->
            if (currentNote != null) {
                // Unified Workspace Canvas (NO TABS) - Single Document Focus
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
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

                        // Interactive Viewport with Skia Handwritten Canvas and Floating Samsung Notes Toolbar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            SkiaHandwrittenCanvas(
                                modifier = Modifier.fillMaxSize(),
                                layers = currentLayers,
                                onLayersChange = { newLayers ->
                                    canvasUndoHistory = canvasUndoHistory + listOf(currentLayers)
                                    canvasRedoHistory = emptyList()
                                    canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to newLayers)
                                },
                                currentBrush = canvasBrush,
                                pendingShapeType = pendingShapeType,
                                onShapePlaced = {
                                    pendingShapeType = null
                                },
                                onCancelShapePlacement = {
                                    pendingShapeType = null
                                }
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
                                    val cleared = currentLayers.map { it.copy(strokes = emptyList(), shapes = emptyList(), textBoxes = emptyList()) }
                                    canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to cleared)
                                },
                                pendingShapeType = pendingShapeType,
                                onSelectShapeForPlacement = { shapeType ->
                                    pendingShapeType = shapeType
                                },
                                onAddTextBox = {
                                    val newBox = CanvasTextBox(
                                        id = "text_${currentTimeMillis()}_${Random.nextInt(10000, 99999)}",
                                        text = "Tap to edit text",
                                        x = 160f,
                                        y = 180f,
                                        width = 160f,
                                        height = 70f
                                    )
                                    canvasUndoHistory = canvasUndoHistory + listOf(currentLayers)
                                    canvasRedoHistory = emptyList()
                                    val updated = if (currentLayers.isEmpty()) {
                                        listOf(
                                            CanvasLayer(
                                                id = "layer_${currentNote.id}",
                                                name = "Main",
                                                textBoxes = listOf(newBox)
                                            )
                                        )
                                    } else {
                                        currentLayers.mapIndexed { i, l ->
                                            if (i == 0) l.copy(textBoxes = l.textBoxes + newBox) else l
                                        }
                                    }
                                    canvasLayersByNoteId = canvasLayersByNoteId + (currentNote.id to updated)
                                },
                                isCollapsed = isCanvasToolbarCollapsed,
                                onToggleCollapse = { isCanvasToolbarCollapsed = !isCanvasToolbarCollapsed },
                                modifier = Modifier.align(if (isCanvasToolbarCollapsed) Alignment.BottomEnd else Alignment.BottomCenter)
                            )
                        }

                        if (showExportCanvasDialog) {
                            ExportCanvasDialog(
                                title = currentNote.title,
                                layers = currentLayers,
                                onDismiss = { showExportCanvasDialog = false }
                            )
                        }
                    } else {
                        // Markdown Text Editor Canvas with Pluggable Engine & Wikilinks (duplicate top row removed)
                        val unpackedMetadata = remember(currentNote.content) {
                            runCatching {
                                if (currentNote.content.startsWith(ProtectedNoteCodec.MAGIC_HEADER)) {
                                    ProtectedNoteCodec.unpack(currentNote.content).metadata
                                } else null
                            }.getOrNull()
                        }
                        val actualSalt = unpackedMetadata?.saltHex ?: "aabbccddeeff00112233445566778899"
                        val actualCheckTag = unpackedMetadata?.passwordCheckTagHex ?: remember { ProtectedNoteCodec.deriveCheckTag("secret123", actualSalt) }
                        val actualHint = unpackedMetadata?.passwordHint ?: "Default demo password: 'secret123'"

                        if (currentNote.isProtected && !unlockedNoteIds.contains(currentNote.id)) {
                            ProtectedNoteBarrier(
                                note = currentNote,
                                passwordHint = actualHint,
                                expectedCheckTagHex = actualCheckTag,
                                saltHex = actualSalt,
                                onUnlocked = {
                                    unlockedNoteIds = unlockedNoteIds + currentNote.id
                                }
                            )
                        } else if (currentNote.isEncrypted && !isVaultUnlocked) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Encrypted Note",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Zero-Knowledge Encrypted Note", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "This note is encrypted using AES-GCM-256. Enter your master passphrase or 12-word BIP-39 recovery kit to decrypt.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { showUnlockVaultDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unlock Note")
                                    }
                                }
                            }
                        } else if (isEditMode) {
                            val displayContent = remember(currentNote.id, currentNote.content) {
                                if (currentNote.isProtected && currentNote.content.startsWith(ProtectedNoteCodec.MAGIC_HEADER)) {
                                    runCatching { ProtectedNoteCodec.unpack(currentNote.content).payloadContent }.getOrDefault(currentNote.content)
                                } else {
                                    currentNote.content
                                }
                            }
                            var editorTextFieldValue by remember(currentNote.id) {
                                mutableStateOf(TextFieldValue(displayContent, TextRange(displayContent.length)))
                            }
                            val content = editorTextFieldValue.text
                            // Check if cursor/content currently has an active [[ autocomplete query
                            val showAutocomplete = content.contains("[[") && !content.substringAfterLast("[[").contains("]")
                            val autocompleteQuery = if (showAutocomplete) content.substringAfterLast("[[").trim() else ""

                            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                EditorToolbar(
                                    value = editorTextFieldValue,
                                    onValueChange = { newValue ->
                                        editorTextFieldValue = newValue
                                        val newStorageContent = if (currentNote.isProtected && unpackedMetadata != null) {
                                            ProtectedNoteCodec.pack(
                                                com.notes.common.models.SelfContainedProtectedNote(
                                                    metadata = unpackedMetadata,
                                                    payloadContent = newValue.text
                                                )
                                            )
                                        } else {
                                            newValue.text
                                        }
                                        val updated = currentNote.copy(content = newStorageContent, updatedAt = currentTimeMillis())
                                        activeNote = updated
                                        notes = notes.map { if (it.id == updated.id) updated else it }
                                        storageRepository.saveNote(updated)
                                    },
                                    activeButtonIds = toolbarConfig.activeButtons,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    OutlinedTextField(
                                        value = editorTextFieldValue,
                                        onValueChange = { newValue ->
                                            editorTextFieldValue = newValue
                                            val newStorageContent = if (currentNote.isProtected && unpackedMetadata != null) {
                                                ProtectedNoteCodec.pack(
                                                    com.notes.common.models.SelfContainedProtectedNote(
                                                        metadata = unpackedMetadata,
                                                        payloadContent = newValue.text
                                                    )
                                                )
                                            } else {
                                                newValue.text
                                            }
                                            val updated = currentNote.copy(content = newStorageContent, updatedAt = currentTimeMillis())
                                            activeNote = updated
                                            notes = notes.map { if (it.id == updated.id) updated else it }
                                            storageRepository.saveNote(updated)
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = editorFontSize.sp),
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
                                                val newTfv = TextFieldValue(newContent, TextRange(newContent.length))
                                                editorTextFieldValue = newTfv
                                                val newStorageContent = if (currentNote.isProtected && unpackedMetadata != null) {
                                                    ProtectedNoteCodec.pack(
                                                        com.notes.common.models.SelfContainedProtectedNote(
                                                            metadata = unpackedMetadata,
                                                            payloadContent = newContent
                                                        )
                                                    )
                                                } else {
                                                    newContent
                                                }
                                                val updated = currentNote.copy(content = newStorageContent, updatedAt = currentTimeMillis())
                                                activeNote = updated
                                                notes = notes.map { if (it.id == updated.id) updated else it }
                                                storageRepository.saveNote(updated)
                                            },
                                            onDismiss = { /* Dismiss popup */ },
                                            modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Pluggable Engine Rendering with Wikilink Navigation
                            val renderContent = if (currentNote.isProtected && currentNote.content.startsWith(ProtectedNoteCodec.MAGIC_HEADER)) {
                                runCatching { ProtectedNoteCodec.unpack(currentNote.content).payloadContent }.getOrDefault(currentNote.content)
                            } else {
                                currentNote.content
                            }
                            currentEngine.Render(
                                content = renderContent,
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
                            text = "Welcome to NotesAlltogether",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No note selected. Select a note from the left sidebar or press Ctrl/Cmd+O to search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = "Create Note",
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

        if (showLoginRequiredDialog) {
            LoginRequiredDialog(
                authManager = authManager,
                onDismiss = { showLoginRequiredDialog = false },
                onSuccess = {
                    showLoginRequiredDialog = false
                    showSettingsDialog = true
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                authManager = authManager,
                deviceSettingsDriver = deviceSettingsDriver,
                onOpenStorageVault = { showStorageDialog = true },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showStorageDialog) {
            StorageVaultDialog(
                repository = storageRepository,
                onDismiss = { showStorageDialog = false }
            )
        }

        if (showUnlockVaultDialog) {
            VaultUnlockDialog(
                noteTitle = activeNote?.title ?: "Vault",
                onDismiss = { showUnlockVaultDialog = false },
                onUnlocked = {
                    isVaultUnlocked = true
                    showUnlockVaultDialog = false
                }
            )
        }

        if (showSmartMetadataDialog && activeMetadataFill != null && activeNote != null) {
            val noteToUpdate = activeNote!!
            val aiSettings = deviceSettingsDriver.getAiSettingsConfig()
            SmartMetadataDialog(
                initialFill = activeMetadataFill!!,
                currentTitle = noteToUpdate.title,
                existingTags = noteToUpdate.tags,
                activeProviderName = when (aiSettings.activeProvider) {
                    AiProviderType.GEMINI -> "Google Gemini"
                    AiProviderType.OPENAI -> "OpenAI"
                    AiProviderType.ANTHROPIC -> "Anthropic Claude"
                    AiProviderType.LOCAL_SERVER -> "Local LLM Server"
                },
                onApply = { newTitle, tagsToAdd, summaryToInsert ->
                    val updatedTitle = if (!newTitle.isNullOrBlank()) newTitle else noteToUpdate.title
                    val updatedTags = (noteToUpdate.tags + tagsToAdd).distinct()
                    val updatedContent = if (!summaryToInsert.isNullOrBlank()) {
                        "> **Summary:** $summaryToInsert\n\n${noteToUpdate.content}"
                    } else {
                        noteToUpdate.content
                    }
                    val updatedNote = noteToUpdate.copy(
                        title = updatedTitle,
                        tags = updatedTags,
                        content = updatedContent
                    )
                    storageRepository.saveNote(updatedNote)
                    notes = notes.map { if (it.id == updatedNote.id) updatedNote else it }
                    activeNote = updatedNote
                    showSmartMetadataDialog = false
                    activeMetadataFill = null
                },
                onDismiss = {
                    showSmartMetadataDialog = false
                    activeMetadataFill = null
                }
            )
        }

        if (isAiLoading) {
            val aiSettings = remember { deviceSettingsDriver.getAiSettingsConfig() }
            AlertDialog(
                onDismissRequest = { /* Modal in-progress */ },
                confirmButton = {},
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Analyzing Note Context")
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Column {
                            Text("Querying ${aiSettings.activeProvider.name}...", style = MaterialTheme.typography.bodyMedium)
                            Text("Generating smart tags, summary, and title...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }

        if (showAiErrorDialog) {
            AlertDialog(
                onDismissRequest = { showAiErrorDialog = false },
                title = { Text("AI Metadata Suggestion") },
                text = {
                    Text(aiErrorMessage ?: "An unexpected error occurred while analyzing note context.")
                },
                confirmButton = {
                    Button(onClick = { showAiErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showPrivacyWarningDialog) {
            val aiSettings = remember { deviceSettingsDriver.getAiSettingsConfig() }
            AlertDialog(
                onDismissRequest = {
                    showPrivacyWarningDialog = false
                    privacyWarningPendingAction = null
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Privacy & Confidentiality Notice")
                    }
                },
                text = {
                    Text("This note is designated as protected or encrypted. Requesting AI suggestions will transmit the sanitized note context to the external cloud provider (${aiSettings.activeProvider.name}).\n\nDo you want to proceed with transmission?")
                },
                confirmButton = {
                    Button(onClick = {
                        showPrivacyWarningDialog = false
                        val action = privacyWarningPendingAction
                        privacyWarningPendingAction = null
                        action?.invoke()
                    }) {
                        Text("Proceed & Analyze")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPrivacyWarningDialog = false
                        privacyWarningPendingAction = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showProtectDialog && activeNote != null) {
            val targetNote = activeNote!!
            SetPasswordProtectionDialog(
                note = targetNote,
                onConfirm = { password, hint ->
                    val protectedSelfContained = ProtectedNoteCodec.createProtectedNote(
                        noteId = targetNote.id,
                        title = targetNote.title,
                        password = password,
                        payloadContent = targetNote.content,
                        type = targetNote.type,
                        passwordHint = hint
                    )
                    val packedContent = ProtectedNoteCodec.pack(protectedSelfContained)
                    val updated = targetNote.copy(
                        content = packedContent,
                        isProtected = true,
                        updatedAt = currentTimeMillis()
                    )
                    storageRepository.saveNote(updated)
                    notes = notes.map { if (it.id == updated.id) updated else it }
                    activeNote = updated
                    unlockedNoteIds = unlockedNoteIds - targetNote.id
                    showProtectDialog = false
                },
                onDismiss = { showProtectDialog = false }
            )
        }

        if (showUnprotectDialog && activeNote != null) {
            val targetNote = activeNote!!
            val unpackedMeta = runCatching {
                if (targetNote.content.startsWith(ProtectedNoteCodec.MAGIC_HEADER)) {
                    ProtectedNoteCodec.unpack(targetNote.content).metadata
                } else null
            }.getOrNull()

            RemovePasswordProtectionDialog(
                note = targetNote,
                expectedCheckTag = unpackedMeta?.passwordCheckTagHex,
                saltHex = unpackedMeta?.saltHex,
                onConfirm = { plainContent ->
                    val updated = targetNote.copy(
                        content = plainContent,
                        isProtected = false,
                        updatedAt = currentTimeMillis()
                    )
                    storageRepository.saveNote(updated)
                    notes = notes.map { if (it.id == updated.id) updated else it }
                    activeNote = updated
                    unlockedNoteIds = unlockedNoteIds + targetNote.id
                    showUnprotectDialog = false
                },
                onDismiss = { showUnprotectDialog = false }
            )
        }

        if (showShareDialog && activeNote != null) {
            val targetNote = activeNote!!
            NoteSharingDialog(
                note = targetNote,
                remoteServerUrl = null,
                onDismiss = { showShareDialog = false },
                onExportPdf = {
                    showExportCanvasDialog = true
                    showShareDialog = false
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
