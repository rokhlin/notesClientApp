package com.notes.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.common.models.Note

/**
 * Master multi-pane workspace scaffold providing the Obsidian UX:
 * 1. Persistent Left Ribbon (52dp)
 * 2. Collapsible Left Sidebar (Vault Tree, Search, Tags, Bookmarks)
 * 3. Minimalist Workspace Canvas with maximized content area
 * 4. Header with Long-Press Rename, AI Metadata, and Edit/View toggle
 * 5. Right Context & Actions Sidebar (Protect, Share, Outline, Backlinks) with right-edge swipe gesture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianScaffold(
    notes: List<Note>,
    activeNote: Note?,
    isDarkTheme: Boolean,
    isEditMode: Boolean = false,
    isNoteUnlocked: Boolean = true,
    activeEngineId: String = "ast-renderer",
    onEngineSelected: ((String) -> Unit)? = null,
    backlinks: List<com.notes.client.editor.Backlink> = emptyList(),
    onBacklinkClick: ((String) -> Unit)? = null,
    onNoteSelected: (Note) -> Unit,
    onCreateNote: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onTriggerAiMetadata: (() -> Unit)? = null,
    onToggleEditMode: (() -> Unit)? = null,
    onRenameNote: ((String) -> Unit)? = null,
    onProtectNote: (() -> Unit)? = null,
    onUnprotectNote: (() -> Unit)? = null,
    onShareNote: (() -> Unit)? = null,
    content: @Composable (Note?) -> Unit
) {
    var isLeftSidebarOpen by remember { mutableStateOf(false) }
    var activeLeftTab by remember { mutableStateOf(ObsidianSidebarTab.FILES) }
    var isRightSidebarOpen by remember { mutableStateOf(false) }
    var isQuickSwitcherOpen by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitleInput by remember(activeNote?.title) { mutableStateOf(activeNote?.title ?: "") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            // Adaptive Compact Layout for Mobile Phones
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = activeNote?.title?.ifBlank { "Untitled Note" } ?: "Notes Alltogether",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .pointerInput(activeNote?.id) {
                                        detectTapGestures(
                                            onLongPress = {
                                                renameTitleInput = activeNote?.title ?: ""
                                                showRenameDialog = true
                                            }
                                        )
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isLeftSidebarOpen = !isLeftSidebarOpen }) {
                                Icon(Icons.Default.Menu, contentDescription = "Navigation Menu")
                            }
                        },
                        actions = {
                            if (activeNote != null && isNoteUnlocked) {
                                if (onTriggerAiMetadata != null) {
                                    IconButton(onClick = onTriggerAiMetadata) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "AI Metadata",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (onToggleEditMode != null && activeNote.type == com.notes.common.models.NoteType.TEXT) {
                                    IconButton(onClick = onToggleEditMode) {
                                        Icon(
                                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                            contentDescription = if (isEditMode) "View Mode" else "Edit Mode",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (activeNote.type == com.notes.common.models.NoteType.CANVAS && onShareNote != null) {
                                    IconButton(onClick = onShareNote) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share Canvas",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isRightSidebarOpen = !isRightSidebarOpen }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Context & Actions",
                                    tint = if (isRightSidebarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
                // NO FOOTER / NAVIGATION BAR: Reclaims full screen height for the note editor!
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                // Swipe from right towards left opens right context panel
                                if (dragAmount < -20f) {
                                    isRightSidebarOpen = true
                                }
                            }
                        }
                ) {
                    content(activeNote)

                    // Left Sidebar Drawer
                    if (isLeftSidebarOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                                .clickable { isLeftSidebarOpen = false }
                        ) {
                            Box(modifier = Modifier.clickable(enabled = false) {}) {
                                ObsidianSidebar(
                                    isOpen = true,
                                    activeTab = activeLeftTab,
                                    notes = notes,
                                    activeNoteId = activeNote?.id,
                                    onTabSelected = { activeLeftTab = it },
                                    onNoteSelected = {
                                        onNoteSelected(it)
                                        isLeftSidebarOpen = false
                                    },
                                    onCollapse = { isLeftSidebarOpen = false },
                                    onCreateFolder = { /* Folder creation handler */ }
                                )
                            }
                        }
                    }

                    // Right Context Drawer for Mobile
                    if (isRightSidebarOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                                .clickable { isRightSidebarOpen = false },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(modifier = Modifier.clickable(enabled = false) {}) {
                                RightContextPanel(
                                    activeNote = activeNote,
                                    backlinks = backlinks,
                                    onBacklinkClick = {
                                        onBacklinkClick?.invoke(it)
                                        isRightSidebarOpen = false
                                    },
                                    onClose = { isRightSidebarOpen = false },
                                    onProtectNote = {
                                        isRightSidebarOpen = false
                                        onProtectNote?.invoke()
                                    },
                                    onUnprotectNote = {
                                        isRightSidebarOpen = false
                                        onUnprotectNote?.invoke()
                                    },
                                    onShareNote = {
                                        isRightSidebarOpen = false
                                        onShareNote?.invoke()
                                    },
                                    onTriggerAiMetadata = onTriggerAiMetadata
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Expanded & Desktop Viewport: Obsidian Multi-Pane Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Left Ribbon (52dp)
                ObsidianRibbon(
                    activeTab = if (isLeftSidebarOpen) activeLeftTab else null,
                    isSidebarOpen = isLeftSidebarOpen,
                    onTabSelected = { tab ->
                        if (isLeftSidebarOpen && activeLeftTab == tab) {
                            isLeftSidebarOpen = false
                        } else {
                            activeLeftTab = tab
                            isLeftSidebarOpen = true
                        }
                    },
                    onOpenQuickSwitcher = { isQuickSwitcherOpen = true },
                    onCreateNote = { onCreateNote("New Note") },
                    onToggleTheme = onToggleTheme,
                    onOpenSettings = onOpenSettings
                )

                // 2. Left Collapsible Sidebar (260dp)
                ObsidianSidebar(
                    isOpen = isLeftSidebarOpen,
                    activeTab = activeLeftTab,
                    notes = notes,
                    activeNoteId = activeNote?.id,
                    onTabSelected = { activeLeftTab = it },
                    onNoteSelected = onNoteSelected,
                    onCollapse = { isLeftSidebarOpen = false },
                    onCreateFolder = { /* Folder creation handler */ }
                )

                // 3. Central Unified Workspace (NO TABS)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Header Toolbar & Breadcrumbs
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Breadcrumb & Long-Press Note Title
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isLeftSidebarOpen) {
                                    IconButton(
                                        onClick = { isLeftSidebarOpen = true },
                                        modifier = Modifier.size(32.dp).padding(end = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open Sidebar", modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text(
                                    text = "Vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " / ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = activeNote?.title?.ifBlank { "Untitled Note" } ?: "No Note Selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .pointerInput(activeNote?.id) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    renameTitleInput = activeNote?.title ?: ""
                                                    showRenameDialog = true
                                                }
                                            )
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Header Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (activeNote != null && isNoteUnlocked) {
                                    if (onTriggerAiMetadata != null) {
                                        IconButton(
                                            onClick = onTriggerAiMetadata,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "AI Metadata",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (onToggleEditMode != null && activeNote.type == com.notes.common.models.NoteType.TEXT) {
                                        IconButton(
                                            onClick = onToggleEditMode,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                                contentDescription = if (isEditMode) "View Mode" else "Edit Mode",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (activeNote.type == com.notes.common.models.NoteType.CANVAS && onShareNote != null) {
                                        IconButton(
                                            onClick = onShareNote,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share Canvas",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { isRightSidebarOpen = !isRightSidebarOpen },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Outline & Info",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isRightSidebarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Main Content Canvas with Swipe Support
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -20f) {
                                        isRightSidebarOpen = true
                                    }
                                }
                            }
                    ) {
                        content(activeNote)
                    }
                }

                // 4. Right Collapsible Context Sidebar (260dp)
                val rightSidebarWidth by animateDpAsState(
                    targetValue = if (isRightSidebarOpen) 260.dp else 0.dp,
                    label = "RightSidebarWidth"
                )

                if (rightSidebarWidth > 0.dp) {
                    Surface(
                        modifier = Modifier
                            .width(rightSidebarWidth)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        RightContextPanel(
                            activeNote = activeNote,
                            backlinks = backlinks,
                            onBacklinkClick = onBacklinkClick,
                            onClose = { isRightSidebarOpen = false },
                            onProtectNote = onProtectNote,
                            onUnprotectNote = onUnprotectNote,
                            onShareNote = onShareNote,
                            onTriggerAiMetadata = onTriggerAiMetadata
                        )
                    }
                }
            }
        }

        // Quick Switcher Dialog
        QuickSwitcherDialog(
            isOpen = isQuickSwitcherOpen,
            notes = notes,
            onDismissRequest = { isQuickSwitcherOpen = false },
            onNoteSelected = { note ->
                onNoteSelected(note)
                isQuickSwitcherOpen = false
            },
            onCreateNote = { title ->
                onCreateNote(title)
                isQuickSwitcherOpen = false
            }
        )

        // Rename Note Dialog
        if (showRenameDialog && activeNote != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter new title for note:")
                        OutlinedTextField(
                            value = renameTitleInput,
                            onValueChange = { renameTitleInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameTitleInput.isNotBlank()) {
                                onRenameNote?.invoke(renameTitleInput.trim())
                                showRenameDialog = false
                            }
                        },
                        enabled = renameTitleInput.isNotBlank()
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun RightContextPanel(
    activeNote: Note?,
    backlinks: List<com.notes.client.editor.Backlink>,
    onBacklinkClick: ((String) -> Unit)?,
    onClose: () -> Unit,
    onProtectNote: (() -> Unit)?,
    onUnprotectNote: (() -> Unit)?,
    onShareNote: (() -> Unit)?,
    onTriggerAiMetadata: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Note Actions & Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Close", modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Note Actions Group
        Text(
            text = "Actions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        if (activeNote != null) {
            if (activeNote.isProtected) {
                OutlinedButton(
                    onClick = { onUnprotectNote?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Password Protection", fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { onProtectNote?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Protect with Password", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { onShareNote?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Note", fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Metadata
        Text(
            text = "Document Info",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        activeNote?.let {
            val wordCount = it.content.split("\\s+".toRegex()).count { w -> w.isNotBlank() }
            Text("Words: $wordCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Type: ${it.type.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (it.tags.isNotEmpty()) {
                Text("Tags: ${it.tags.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Backlinks
        if (backlinks.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = "Backlinks (${backlinks.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                backlinks.forEach { backlink ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBacklinkClick?.invoke(backlink.sourceNoteId) },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(text = backlink.sourceNoteTitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = backlink.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
