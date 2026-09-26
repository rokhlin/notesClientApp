package com.notes.client.components

import androidx.compose.foundation.layout.*
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
                        icon = { Text("📝") },
                        label = { Text("Notes") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "canvas",
                        onClick = { onNavigate("canvas") },
                        icon = { Text("🎨") },
                        label = { Text("Canvas") }
                    )
                    NavigationRailItem(
                        selected = currentRoute == "settings",
                        onClick = { onNavigate("settings") },
                        icon = { Text("⚙️") },
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
                            icon = { Text("📝") },
                            label = { Text("Notes") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "canvas",
                            onClick = { onNavigate("canvas") },
                            icon = { Text("🎨") },
                            label = { Text("Canvas") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { onNavigate("settings") },
                            icon = { Text("⚙️") },
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
