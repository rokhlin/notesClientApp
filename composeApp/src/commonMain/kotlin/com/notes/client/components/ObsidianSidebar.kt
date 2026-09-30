package com.notes.client.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.common.models.Note

/**
 * Collapsible left navigation sidebar with animated width and tab switching
 * between File Tree, Tags Hierarchy, and Bookmarks.
 */
@Composable
fun ObsidianSidebar(
    isOpen: Boolean,
    activeTab: ObsidianSidebarTab,
    notes: List<Note>,
    activeNoteId: String?,
    onTabSelected: (ObsidianSidebarTab) -> Unit,
    onNoteSelected: (Note) -> Unit,
    onCollapse: () -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isOpen) 260.dp else 0.dp,
        label = "SidebarWidthAnimation"
    )

    if (sidebarWidth <= 0.dp) return

    var searchQuery by remember { mutableStateOf("") }
    var searchContentEnabled by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (activeTab) {
                        ObsidianSidebarTab.FILES -> "Vault Files"
                        ObsidianSidebarTab.SEARCH -> "Search Notes"
                        ObsidianSidebarTab.TAGS -> "Tags Hierarchy"
                        ObsidianSidebarTab.BOOKMARKS -> "Bookmarks"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCreateFolder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Collapse Sidebar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tab Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabPill(
                        title = "Files",
                        isSelected = activeTab == ObsidianSidebarTab.FILES,
                        onClick = { onTabSelected(ObsidianSidebarTab.FILES) }
                    )
                    TabPill(
                        title = "Search",
                        isSelected = activeTab == ObsidianSidebarTab.SEARCH,
                        onClick = { onTabSelected(ObsidianSidebarTab.SEARCH) }
                    )
                    TabPill(
                        title = "Tags",
                        isSelected = activeTab == ObsidianSidebarTab.TAGS,
                        onClick = { onTabSelected(ObsidianSidebarTab.TAGS) }
                    )
                    TabPill(
                        title = "Stars",
                        isSelected = activeTab == ObsidianSidebarTab.BOOKMARKS,
                        onClick = { onTabSelected(ObsidianSidebarTab.BOOKMARKS) }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Content Body
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    ObsidianSidebarTab.SEARCH -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search notes...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = searchContentEnabled,
                                    onCheckedChange = { searchContentEnabled = it },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Search inside content",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val filteredNotes = remember(searchQuery, searchContentEnabled, notes) {
                                val q = searchQuery.trim()
                                if (q.isBlank()) {
                                    notes
                                } else {
                                    notes.filter { note ->
                                        val matchesTitle = note.title.contains(q, ignoreCase = true)
                                        val matchesTag = note.tags.any { it.contains(q, ignoreCase = true) }
                                        val matchesContent = if (searchContentEnabled && !note.isProtected && !note.isEncrypted) {
                                            note.content.contains(q, ignoreCase = true)
                                        } else false
                                        matchesTitle || matchesTag || matchesContent
                                    }
                                }
                            }

                            if (filteredNotes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No notes matched \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredNotes) { note ->
                                        val isSelected = note.id == activeNoteId
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onNoteSelected(note) },
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (note.type == com.notes.common.models.NoteType.CANVAS) Icons.Default.Brush else Icons.AutoMirrored.Filled.Article,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = note.title.ifBlank { "Untitled" },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (note.isProtected || note.isEncrypted) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = "Protected",
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                if (note.tags.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier.padding(top = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        note.tags.take(3).forEach { tag ->
                                                            Text(
                                                                text = "#$tag",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
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
                    ObsidianSidebarTab.FILES -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(notes) { note ->
                                val isSelected = note.id == activeNoteId
                                NoteTreeRow(
                                    note = note,
                                    isSelected = isSelected,
                                    onClick = { onNoteSelected(note) }
                                )
                            }
                        }
                    }
                    ObsidianSidebarTab.TAGS -> {
                        val tagsMap = remember(notes) {
                            notes.flatMap { it.tags }
                                .groupingBy { it }
                                .eachCount()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(tagsMap.entries.toList()) { (tag, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ObsidianSidebarTab.BOOKMARKS -> {
                        val bookmarkedNotes = remember(notes) {
                            notes.filter { it.tags.contains("starred") || it.tags.contains("favorite") }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (bookmarkedNotes.isEmpty()) {
                                item {
                                    Text(
                                        text = "No bookmarked notes yet. Tag notes with #starred to see them here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(bookmarkedNotes) { note ->
                                    NoteTreeRow(
                                        note = note,
                                        isSelected = note.id == activeNoteId,
                                        onClick = { onNoteSelected(note) }
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

@Composable
private fun RowScope.TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun NoteTreeRow(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
               else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (note.type == com.notes.common.models.NoteType.CANVAS) Icons.Default.Brush else Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
