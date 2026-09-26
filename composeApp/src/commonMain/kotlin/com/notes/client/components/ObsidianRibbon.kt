package com.notes.client.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ObsidianSidebarTab {
    FILES,
    TAGS,
    BOOKMARKS
}

/**
 * Slim 52dp persistent vertical action strip inspired by Obsidian's left ribbon.
 * Houses primary workspace navigation and overlay triggers.
 */
@Composable
fun ObsidianRibbon(
    activeTab: ObsidianSidebarTab?,
    isSidebarOpen: Boolean,
    onTabSelected: (ObsidianSidebarTab) -> Unit,
    onOpenQuickSwitcher: () -> Unit,
    onCreateNote: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(52.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Action Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // File Explorer
                IconButton(
                    onClick = { onTabSelected(ObsidianSidebarTab.FILES) },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isSidebarOpen && activeTab == ObsidianSidebarTab.FILES) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        }
                    )
                ) {
                    Text(
                        text = "📁",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Quick Switcher
                IconButton(
                    onClick = onOpenQuickSwitcher,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text(
                        text = "🔍",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Tags Explorer
                IconButton(
                    onClick = { onTabSelected(ObsidianSidebarTab.TAGS) },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isSidebarOpen && activeTab == ObsidianSidebarTab.TAGS) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        }
                    )
                ) {
                    Text(
                        text = "🏷️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Bookmarks
                IconButton(
                    onClick = { onTabSelected(ObsidianSidebarTab.BOOKMARKS) },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isSidebarOpen && activeTab == ObsidianSidebarTab.BOOKMARKS) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        }
                    )
                ) {
                    Text(
                        text = "⭐",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .width(28.dp)
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Create Note Button
                FilledIconButton(
                    onClick = onCreateNote,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "＋",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Bottom Action Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Theme Toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text(
                        text = "🌓",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text(
                        text = "⚙️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
