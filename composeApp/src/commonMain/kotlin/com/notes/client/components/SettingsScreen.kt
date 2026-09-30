package com.notes.client.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.client.auth.AuthManager
import com.notes.client.editor.ToolbarRegistry
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.common.models.GeneralSettingsConfig
import com.notes.common.models.StorageBackendType
import com.notes.common.models.ToolbarConfig

enum class SettingsSubPage(val title: String, val subtitle: String, val icon: ImageVector) {
    GENERAL("General", "Theme, editor font size, search options", Icons.Default.Tune),
    VAULT("Vault", "Cloud Profile & Storage Paths", Icons.Default.Cloud),
    AI_PROVIDERS("AI Providers", "Gemini, OpenAI, Claude, Local LLMs", Icons.Default.AutoAwesome),
    EDITOR_TOOLBAR("Editor Toolbar", "Customize and reorder formatting buttons", Icons.Default.Build)
}

/**
 * Dedicated Full-Page Responsive Settings Screen.
 * - Portrait (< 600dp): Separate drill-down navigation screens with Back button.
 * - Landscape / Foldable / Tablet (>= 600dp): Master-detail view with left navigation menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authManager: AuthManager,
    deviceSettingsDriver: DeviceSettingsDriver,
    onNavigateBack: () -> Unit,
    onOpenStorageVault: (() -> Unit)? = null,
    onThemeChanged: ((String) -> Unit)? = null,
    onFontSizeChanged: ((Float) -> Unit)? = null,
    onToolbarConfigChanged: (() -> Unit)? = null
) {
    var selectedSubPage by remember { mutableStateOf(SettingsSubPage.GENERAL) }
    var portraitSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // Landscape / Foldable / Tablet: Master-Detail Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Master Menu (260dp)
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header with Back button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Notes")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Navigation list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SettingsSubPage.values().forEach { subPage ->
                                val isSelected = selectedSubPage == subPage
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSubPage = subPage },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = subPage.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = subPage.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        val profile = authManager.currentProfile
                        if (profile != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        authManager.logout()
                                        onNavigateBack()
                                    }
                                ) {
                                    Text("Logout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Right Detail View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    Text(
                        text = selectedSubPage.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedSubPage.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selectedSubPage) {
                            SettingsSubPage.GENERAL -> GeneralSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                onThemeChanged = onThemeChanged,
                                onFontSizeChanged = onFontSizeChanged
                            )
                            SettingsSubPage.VAULT -> VaultSettingsView(
                                authManager = authManager,
                                onOpenStorageVault = onOpenStorageVault
                            )
                            SettingsSubPage.AI_PROVIDERS -> AiProviderSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                authManager = authManager
                            )
                            SettingsSubPage.EDITOR_TOOLBAR -> EditorToolbarSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                onToolbarConfigChanged = onToolbarConfigChanged
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait: Drill-Down Navigation
            if (portraitSubPage == null) {
                // Settings Root List
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsSubPage.values().forEach { subPage ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { portraitSubPage = subPage },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = subPage.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subPage.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = subPage.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        val profile = authManager.currentProfile
                        if (profile != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    authManager.logout()
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Out (${profile.email})")
                            }
                        }
                    }
                }
            } else {
                // Drill-Down Subpage
                val currentSub = portraitSubPage!!
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(currentSub.title, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { portraitSubPage = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        when (currentSub) {
                            SettingsSubPage.GENERAL -> GeneralSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                onThemeChanged = onThemeChanged,
                                onFontSizeChanged = onFontSizeChanged
                            )
                            SettingsSubPage.VAULT -> VaultSettingsView(
                                authManager = authManager,
                                onOpenStorageVault = onOpenStorageVault
                            )
                            SettingsSubPage.AI_PROVIDERS -> AiProviderSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                authManager = authManager
                            )
                            SettingsSubPage.EDITOR_TOOLBAR -> EditorToolbarSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                onToolbarConfigChanged = onToolbarConfigChanged
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsView(
    deviceSettingsDriver: DeviceSettingsDriver,
    onThemeChanged: ((String) -> Unit)? = null,
    onFontSizeChanged: ((Float) -> Unit)? = null
) {
    val initialConfig = remember { deviceSettingsDriver.getGeneralSettings() }
    var currentTheme by remember { mutableStateOf(initialConfig.theme) }
    var currentFontSize by remember { mutableStateOf(initialConfig.editorFontSize) }
    var searchContentEnabled by remember { mutableStateOf(initialConfig.searchContentEnabled) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (statusMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // 1. Theme Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Color Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System").forEach { (code, label) ->
                        FilterChip(
                            selected = currentTheme == code,
                            onClick = {
                                currentTheme = code
                                onThemeChanged?.invoke(code)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 2. Editor Font Size
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Editor Font Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${currentFontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(14f to "Small", 16f to "Medium", 18f to "Large", 20f to "Extra Large").forEach { (size, label) ->
                        FilterChip(
                            selected = currentFontSize == size,
                            onClick = {
                                currentFontSize = size
                                onFontSizeChanged?.invoke(size)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 3. Search Note Content Checkbox
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Search Note Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Include body text of notes in search queries. Protected notes are excluded from content search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = searchContentEnabled,
                    onCheckedChange = { searchContentEnabled = it }
                )
            }
        }

        Button(
            onClick = {
                val newConfig = GeneralSettingsConfig(
                    theme = currentTheme,
                    editorFontSize = currentFontSize,
                    searchContentEnabled = searchContentEnabled
                )
                deviceSettingsDriver.saveGeneralSettings(newConfig)
                statusMessage = "General settings saved successfully!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save General Settings")
        }
    }
}

@Composable
fun VaultSettingsView(
    authManager: AuthManager,
    onOpenStorageVault: (() -> Unit)? = null
) {
    val profile = authManager.currentProfile
    val cloudConfig = authManager.currentCloudConfig ?: AuthManager.defaultCloudConfig(profile?.userId ?: "usr_demo", profile?.email ?: "user@demo.org")

    var localVaultPath by remember { mutableStateOf(cloudConfig.storagePaths.localVaultPath) }
    var remoteStorageUrl by remember { mutableStateOf(cloudConfig.storagePaths.remoteStorageUrl ?: "") }
    var storageBackend by remember { mutableStateOf(cloudConfig.storagePaths.storageBackendType) }
    var autoSyncEnabled by remember { mutableStateOf(cloudConfig.storagePaths.autoSyncEnabled) }
    var enabledModules by remember { mutableStateOf(cloudConfig.enabledModules.toSet()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (statusMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = localVaultPath,
            onValueChange = { localVaultPath = it },
            label = { Text("Local Vault Root Path") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = remoteStorageUrl,
            onValueChange = { remoteStorageUrl = it },
            label = { Text("Remote Cloud Storage / Sync URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Storage Backend:", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StorageBackendType.values().forEach { backend ->
                FilterChip(
                    selected = storageBackend == backend,
                    onClick = { storageBackend = backend },
                    label = { Text(backend.name) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Auto-Sync on Changes", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = autoSyncEnabled,
                onCheckedChange = { autoSyncEnabled = it }
            )
        }

        Text("Pluggable Modules:", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("core-editor", "skia-canvas", "premium-tables", "pdf-export").forEach { modId ->
                val isChecked = enabledModules.contains(modId)
                FilterChip(
                    selected = isChecked,
                    onClick = {
                        enabledModules = if (isChecked) enabledModules - modId else enabledModules + modId
                    },
                    label = { Text(modId) }
                )
            }
        }

        if (onOpenStorageVault != null) {
            OutlinedButton(
                onClick = onOpenStorageVault,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inspect Sandboxed Vault & Index")
            }
        }

        Button(
            onClick = {
                val updated = cloudConfig.copy(
                    storagePaths = cloudConfig.storagePaths.copy(
                        localVaultPath = localVaultPath,
                        remoteStorageUrl = remoteStorageUrl.ifBlank { null },
                        storageBackendType = storageBackend,
                        autoSyncEnabled = autoSyncEnabled
                    ),
                    enabledModules = enabledModules.toList()
                )
                authManager.updateCloudConfig(updated)
                statusMessage = "Cloud profile & paths saved successfully!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Cloud Profile & Paths")
        }
    }
}

@Composable
fun EditorToolbarSettingsView(
    deviceSettingsDriver: DeviceSettingsDriver,
    onToolbarConfigChanged: (() -> Unit)? = null
) {
    val initialConfig = remember { deviceSettingsDriver.getToolbarConfig() }
    var activeButtons by remember { mutableStateOf(initialConfig.activeButtons) }
    var disabledButtons by remember { mutableStateOf(initialConfig.disabledButtons) }
    var customCommands by remember { mutableStateOf(initialConfig.customCommands) }
    var showAddCommandDialog by remember { mutableStateOf(false) }
    var newCommandName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun persistConfig() {
        val updated = ToolbarConfig(
            activeButtons = activeButtons,
            disabledButtons = disabledButtons,
            customCommands = customCommands
        )
        deviceSettingsDriver.saveToolbarConfig(updated)
        onToolbarConfigChanged?.invoke()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (statusMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Active Buttons Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Toolbar Buttons (${activeButtons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        activeButtons = ToolbarRegistry.DEFAULT_BUTTON_IDS
                        disabledButtons = emptyList()
                        customCommands = emptyList()
                        persistConfig()
                        statusMessage = "Reset toolbar to defaults!"
                    }
                ) {
                    Text("Reset Defaults")
                }
            }
        }

        // Active Buttons List
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeButtons.isEmpty()) {
                    Text("No buttons on toolbar. Add buttons from below.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                activeButtons.forEachIndexed { index, buttonId ->
                    val actionItem = ToolbarRegistry.ALL_ACTIONS[buttonId]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                if (actionItem?.icon != null) {
                                    Icon(actionItem.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(actionItem?.label ?: buttonId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(actionItem?.title ?: buttonId, style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = activeButtons.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index - 1]
                                            mutable[index - 1] = temp
                                            activeButtons = mutable
                                            persistConfig()
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index < activeButtons.size - 1) {
                                            val mutable = activeButtons.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index + 1]
                                            mutable[index + 1] = temp
                                            activeButtons = mutable
                                            persistConfig()
                                        }
                                    },
                                    enabled = index < activeButtons.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        activeButtons = activeButtons - buttonId
                                        disabledButtons = disabledButtons + buttonId
                                        persistConfig()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove from Toolbar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Disabled / Hidden Buttons Section
        Text("Hidden / Available Buttons (${disabledButtons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (disabledButtons.isEmpty()) {
                    Text("All buttons are currently active on the toolbar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        disabledButtons.forEach { buttonId ->
                            val actionItem = ToolbarRegistry.ALL_ACTIONS[buttonId]
                            AssistChip(
                                onClick = {
                                    disabledButtons = disabledButtons - buttonId
                                    activeButtons = activeButtons + buttonId
                                    persistConfig()
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(actionItem?.title ?: buttonId) }
                            )
                        }
                    }
                }
            }
        }

        // Add custom command
        OutlinedButton(
            onClick = { showAddCommandDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add New Custom Command")
        }

        if (showAddCommandDialog) {
            AlertDialog(
                onDismissRequest = { showAddCommandDialog = false },
                title = { Text("Add Custom Command") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add a custom Markdown snippet or formatting action to the editor toolbar.")
                        OutlinedTextField(
                            value = newCommandName,
                            onValueChange = { newCommandName = it },
                            label = { Text("Command Identifier / Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCommandName.isNotBlank()) {
                                customCommands = customCommands + newCommandName.trim()
                                activeButtons = activeButtons + newCommandName.trim()
                                persistConfig()
                                newCommandName = ""
                                showAddCommandDialog = false
                            }
                        },
                        enabled = newCommandName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCommandDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
