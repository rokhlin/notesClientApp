package com.notes.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notes.common.models.Note

/**
 * Master multi-pane workspace scaffold providing the Obsidian UX:
 * 1. Persistent Left Ribbon (52dp)
 * 2. Collapsible Left Sidebar (Vault Tree, Tags, Bookmarks)
 * 3. Unified No-Tabs Workspace Canvas with breadcrumbs
 * 4. Collapsible Right Context Sidebar (Outline, Backlinks, Metadata)
 * 5. Integrated Quick Switcher Modal Dialog (Ctrl/Cmd + O)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianScaffold(
    notes: List<Note>,
    activeNote: Note?,
    isDarkTheme: Boolean,
    activeEngineId: String = "ast-renderer",
    onEngineSelected: ((String) -> Unit)? = null,
    backlinks: List<com.notes.client.editor.Backlink> = emptyList(),
    onBacklinkClick: ((String) -> Unit)? = null,
    onNoteSelected: (Note) -> Unit,
    onCreateNote: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (Note?) -> Unit
) {
    var isLeftSidebarOpen by remember { mutableStateOf(true) }
    var activeLeftTab by remember { mutableStateOf(ObsidianSidebarTab.FILES) }
    var isRightSidebarOpen by remember { mutableStateOf(false) }
    var isQuickSwitcherOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            // Adaptive Compact Fallback for Phone Viewports
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = activeNote?.title ?: "Notes Alltogether",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isLeftSidebarOpen = !isLeftSidebarOpen }) {
                                Text("📁")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isQuickSwitcherOpen = true }) {
                                Text("🔍")
                            }
                            IconButton(onClick = onToggleTheme) {
                                Text(if (isDarkTheme) "☀️" else "🌙")
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = activeLeftTab == ObsidianSidebarTab.FILES,
                            onClick = {
                                activeLeftTab = ObsidianSidebarTab.FILES
                                isLeftSidebarOpen = true
                            },
                            icon = { Text("📁") },
                            label = { Text("Vault") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { isQuickSwitcherOpen = true },
                            icon = { Text("🔍") },
                            label = { Text("Search") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = onOpenSettings,
                            icon = { Text("⚙️") },
                            label = { Text("Settings") }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content(activeNote)

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
                            // Breadcrumb
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isLeftSidebarOpen) {
                                    IconButton(
                                        onClick = { isLeftSidebarOpen = true },
                                        modifier = Modifier.size(32.dp).padding(end = 6.dp)
                                    ) {
                                        Text("☰", style = MaterialTheme.typography.labelMedium)
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
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeNote?.let {
                                    val wordCount = it.content.split("\\s+".toRegex()).count { w -> w.isNotBlank() }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "$wordCount words",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (onEngineSelected != null && activeNote?.type == com.notes.common.models.NoteType.TEXT) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(modifier = Modifier.padding(2.dp)) {
                                            Surface(
                                                modifier = Modifier.clickable { onEngineSelected("ast-renderer") },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (activeEngineId == "ast-renderer") MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                                            ) {
                                                Text(
                                                    text = "AST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeEngineId == "ast-renderer") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                modifier = Modifier.clickable { onEngineSelected("richtext-wysiwyg") },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (activeEngineId == "richtext-wysiwyg") MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                                            ) {
                                                Text(
                                                    text = "WYSIWYG",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeEngineId == "richtext-wysiwyg") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { isRightSidebarOpen = !isRightSidebarOpen },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("📖", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Main Content Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        content(activeNote)
                    }
                }

                // 4. Right Collapsible Context Sidebar (240dp)
                val rightSidebarWidth by animateDpAsState(
                    targetValue = if (isRightSidebarOpen) 240.dp else 0.dp,
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Outline & Info",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { isRightSidebarOpen = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("▶", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Text(
                                text = "Headings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "H1: ${activeNote?.title ?: "Document"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Metadata",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type: ${activeNote?.type?.name ?: "N/A"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tags: ${activeNote?.tags?.joinToString() ?: "None"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (backlinks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Backlinks (${backlinks.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
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
                                                Text(
                                                    text = "🔗 " + backlink.sourceNoteTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
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
                }
            }
        }

        // 5. Quick Switcher Floating Modal Dialog (Ctrl/Cmd + O)
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
    }
}
