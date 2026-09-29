package com.notes.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(80.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    NavigationRailItem(
                        selected = currentRoute == "notes",
                        onClick = { onNavigate("notes") },
                        icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "Notes") },
                        label = { Text("Notes") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "canvas",
                        onClick = { onNavigate("canvas") },
                        icon = { Icon(Icons.Default.Brush, contentDescription = "Canvas") },
                        label = { Text("Canvas") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "settings",
                        onClick = { onNavigate("settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    floatingActionButton = floatingActionButton,
                    content = content
                )
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "notes",
                            onClick = { onNavigate("notes") },
                            icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "Notes") },
                            label = { Text("Notes") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "canvas",
                            onClick = { onNavigate("canvas") },
                            icon = { Icon(Icons.Default.Brush, contentDescription = "Canvas") },
                            label = { Text("Canvas") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { onNavigate("settings") },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                },
                floatingActionButton = floatingActionButton,
                content = content
            )
        }
    }
}
