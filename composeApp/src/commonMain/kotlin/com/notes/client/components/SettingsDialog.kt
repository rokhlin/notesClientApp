package com.notes.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.notes.client.ai.AiClientService
import com.notes.client.ai.DefaultAiClientService
import com.notes.client.auth.AuthManager
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.common.models.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Composable
fun SettingsDialog(
    authManager: AuthManager,
    deviceSettingsDriver: DeviceSettingsDriver,
    onOpenStorageVault: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val profile = authManager.currentProfile
    val cloudConfig = authManager.currentCloudConfig ?: AuthManager.defaultCloudConfig(profile?.userId ?: "usr_demo", profile?.email ?: "user@demo.org")
    val localDeviceConfig = remember { deviceSettingsDriver.getDeviceModuleConfig("core") }

    // Tier 1 State
    var localVaultPath by remember { mutableStateOf(cloudConfig.storagePaths.localVaultPath) }
    var remoteStorageUrl by remember { mutableStateOf(cloudConfig.storagePaths.remoteStorageUrl ?: "") }
    var storageBackend by remember { mutableStateOf(cloudConfig.storagePaths.storageBackendType) }
    var autoSyncEnabled by remember { mutableStateOf(cloudConfig.storagePaths.autoSyncEnabled) }
    var enabledModules by remember { mutableStateOf(cloudConfig.enabledModules.toSet()) }
    var cloudSaveStatus by remember { mutableStateOf<String?>(null) }

    // Tier 2 State
    var isHwAccel by remember { mutableStateOf(localDeviceConfig.isHardwareAccelerationEnabled) }
    var stylusCurve by remember { mutableStateOf(localDeviceConfig.stylusPressureCurve) }
    var localCacheDir by remember { mutableStateOf(localDeviceConfig.localCacheDirectory) }
    var deviceSaveStatus by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(680.dp)
                .fillMaxHeight(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "System Configuration",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Authenticated user: ${profile?.email ?: "Local User"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        authManager.logout()
                        onDismiss()
                    }) {
                        Text("Log Out", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Cloud Profile & Storage")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Device & Hardware")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("AI Providers")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // TIER 1: USER CLOUD PROFILE
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (cloudSaveStatus != null) {
                                Text(
                                    text = cloudSaveStatus ?: "",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
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

                            // Storage Backend Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Storage Backend:", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StorageBackendType.values().forEach { backend ->
                                        FilterChip(
                                            selected = storageBackend == backend,
                                            onClick = { storageBackend = backend },
                                            label = { Text(backend.name) }
                                        )
                                    }
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

                            // Enabled Modules (Modular Constructor)
                            Text("Pluggable Modules (Constructor):", style = MaterialTheme.typography.titleSmall)
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

                            Spacer(modifier = Modifier.weight(1f))
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
                                    cloudSaveStatus = "Cloud configuration saved successfully!"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Cloud Profile & Paths")
                            }
                        }
                    }
                    1 -> {
                            // TIER 2: DEVICE LOCAL RUNTIME SETTINGS
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Hardware settings are stored only on this physical device and never uploaded to cloud.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (deviceSaveStatus != null) {
                                    Text(
                                        text = deviceSaveStatus ?: "",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Hardware Graphics Acceleration (GPU)", style = MaterialTheme.typography.bodyMedium)
                                        Text("Skia rendering with OpenGL/Vulkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isHwAccel,
                                        onCheckedChange = { isHwAccel = it }
                                    )
                                }

                                Column {
                                    Text("Stylus Pressure Sensitivity Curve: ${((stylusCurve * 100).toInt())}%", style = MaterialTheme.typography.bodyMedium)
                                    Slider(
                                        value = stylusCurve,
                                        onValueChange = { stylusCurve = it },
                                        valueRange = 0.5f..2.0f
                                    )
                                }

                                OutlinedTextField(
                                    value = localCacheDir,
                                    onValueChange = { localCacheDir = it },
                                    label = { Text("Device Cache Directory") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        val updatedDevice = localDeviceConfig.copy(
                                            isHardwareAccelerationEnabled = isHwAccel,
                                            stylusPressureCurve = stylusCurve,
                                            localCacheDirectory = localCacheDir
                                        )
                                        deviceSettingsDriver.saveDeviceModuleConfig(updatedDevice)
                                        deviceSaveStatus = "Device settings saved to local hardware storage!"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Device Hardware Settings")
                                }
                            }
                        }
                        else -> {
                            // TAB 2: AI PROVIDER CONNECTIONS
                            AiProviderSettingsView(
                                deviceSettingsDriver = deviceSettingsDriver,
                                authManager = authManager
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AiProviderSettingsView(
    deviceSettingsDriver: DeviceSettingsDriver,
    authManager: AuthManager
) {
    val initialAiConfig = remember { deviceSettingsDriver.getAiSettingsConfig() }
    var activeProvider by remember { mutableStateOf(initialAiConfig.activeProvider) }
    var providersMap by remember { mutableStateOf(initialAiConfig.providers) }
    var autoSuggestTags by remember { mutableStateOf(initialAiConfig.autoSuggestOnNoteCreation) }
    var maxTags by remember { mutableStateOf(initialAiConfig.maxTagsToGenerate) }
    var selectedProviderType by remember { mutableStateOf(activeProvider) }
    var aiSaveStatus by remember { mutableStateOf<String?>(null) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isDiscoveringModels by remember { mutableStateOf(false) }
    var discoveredModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var discoveryMessage by remember { mutableStateOf<String?>(null) }
    var showApiKey by remember { mutableStateOf(false) }

    val aiClientService = remember { DefaultAiClientService() }
    val coroutineScope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true } }
    val aiCatalog = remember { AiModelCatalog.defaultCatalog() }

    val currentConfig = providersMap[selectedProviderType] ?: AiProviderConfig(providerType = selectedProviderType)

    fun updateCurrentConfig(updated: AiProviderConfig) {
        providersMap = providersMap + (selectedProviderType to updated)
        connectionTestResult = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (aiSaveStatus != null) {
            Text(
                text = aiSaveStatus ?: "",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "Configure AI connections for smart metadata filling, tagging, and contextual summaries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Provider Selector Chips
        Text("AI Provider:", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiProviderType.values().forEach { providerType ->
                val isSelected = selectedProviderType == providerType
                val isActive = activeProvider == providerType
                val label = when (providerType) {
                    AiProviderType.GEMINI -> "Gemini"
                    AiProviderType.OPENAI -> "OpenAI"
                    AiProviderType.ANTHROPIC -> "Claude"
                    AiProviderType.LOCAL_SERVER -> "Local LLM"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedProviderType = providerType
                        connectionTestResult = null
                        discoveryMessage = null
                    },
                    label = { Text(if (isActive) "$label (Active)" else label) }
                )
            }
        }

        // Active Provider Status / Make Active Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (activeProvider == selectedProviderType)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeProvider == selectedProviderType) "Default Provider" else "Inactive for Note Actions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Used when triggering AI Metadata from editor or sidebar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (activeProvider != selectedProviderType) {
                    Button(onClick = { activeProvider = selectedProviderType }) {
                        Text("Set as Active")
                    }
                }
            }
        }

        // Provider-Specific Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedProviderType) {
                    AiProviderType.GEMINI -> {
                        // API Key with eye toggle
                        OutlinedTextField(
                            value = currentConfig.apiKey,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(apiKey = it)) },
                            label = { Text("Google Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Text(if (showApiKey) "👁️" else "🙈")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Primary Model Selection
                        Text("Primary Model:", style = MaterialTheme.typography.labelMedium)
                        val geminiModels = aiCatalog.getModelsForProvider(AiProviderType.GEMINI)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            geminiModels.forEach { modelEntry ->
                                FilterChip(
                                    selected = currentConfig.primaryModelId == modelEntry.id,
                                    onClick = { updateCurrentConfig(currentConfig.copy(primaryModelId = modelEntry.id)) },
                                    label = { Text(if (modelEntry.isRecommended) "${modelEntry.displayName} (Recommended)" else modelEntry.displayName) }
                                )
                            }
                        }

                        // Fallback Model Selection
                        Text("Failover Fallback Model:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            geminiModels.forEach { modelEntry ->
                                FilterChip(
                                    selected = currentConfig.fallbackModelId == modelEntry.id,
                                    onClick = { updateCurrentConfig(currentConfig.copy(fallbackModelId = modelEntry.id)) },
                                    label = { Text(if (modelEntry.tier == AiModelTier.FALLBACK) "${modelEntry.displayName} (Fallback)" else modelEntry.displayName) }
                                )
                            }
                            FilterChip(
                                selected = currentConfig.fallbackModelId == null,
                                onClick = { updateCurrentConfig(currentConfig.copy(fallbackModelId = null)) },
                                label = { Text("None") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Failover Resiliency", style = MaterialTheme.typography.bodyMedium)
                                Text("Automatically fallback if primary model is unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = currentConfig.isFallbackEnabled,
                                onCheckedChange = { updateCurrentConfig(currentConfig.copy(isFallbackEnabled = it)) }
                            )
                        }
                    }

                    AiProviderType.OPENAI -> {
                        OutlinedTextField(
                            value = currentConfig.apiKey,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(apiKey = it)) },
                            label = { Text("OpenAI API Key") },
                            placeholder = { Text("sk-proj-...") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Text(if (showApiKey) "👁️" else "🙈")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Primary Model:", style = MaterialTheme.typography.labelMedium)
                        val openAiModels = aiCatalog.getModelsForProvider(AiProviderType.OPENAI)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            openAiModels.forEach { modelEntry ->
                                FilterChip(
                                    selected = currentConfig.primaryModelId == modelEntry.id,
                                    onClick = { updateCurrentConfig(currentConfig.copy(primaryModelId = modelEntry.id)) },
                                    label = { Text(if (modelEntry.isRecommended) "${modelEntry.displayName} (Recommended)" else modelEntry.displayName) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = currentConfig.baseUrl,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(baseUrl = it)) },
                            label = { Text("OpenAI Base URL / Gateway") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AiProviderType.ANTHROPIC -> {
                        OutlinedTextField(
                            value = currentConfig.apiKey,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(apiKey = it)) },
                            label = { Text("Anthropic API Key") },
                            placeholder = { Text("sk-ant-...") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Text(if (showApiKey) "👁️" else "🙈")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Primary Model:", style = MaterialTheme.typography.labelMedium)
                        val anthropicModels = aiCatalog.getModelsForProvider(AiProviderType.ANTHROPIC)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            anthropicModels.forEach { modelEntry ->
                                FilterChip(
                                    selected = currentConfig.primaryModelId == modelEntry.id,
                                    onClick = { updateCurrentConfig(currentConfig.copy(primaryModelId = modelEntry.id)) },
                                    label = { Text(if (modelEntry.isRecommended) "${modelEntry.displayName} (Recommended)" else modelEntry.displayName) }
                                )
                            }
                        }
                    }

                    AiProviderType.LOCAL_SERVER -> {
                        OutlinedTextField(
                            value = currentConfig.baseUrl,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(baseUrl = it)) },
                            label = { Text("Local Server Endpoint URL") },
                            placeholder = { Text("http://localhost:11434") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Protocol:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentConfig.localProtocol == LocalAiProtocol.OLLAMA_NATIVE,
                                onClick = { updateCurrentConfig(currentConfig.copy(localProtocol = LocalAiProtocol.OLLAMA_NATIVE)) },
                                label = { Text("Ollama Native (/api/generate)") }
                            )
                            FilterChip(
                                selected = currentConfig.localProtocol == LocalAiProtocol.OPENAI_COMPATIBLE,
                                onClick = { updateCurrentConfig(currentConfig.copy(localProtocol = LocalAiProtocol.OPENAI_COMPATIBLE)) },
                                label = { Text("OpenAI Compatible (/v1/chat)") }
                            )
                        }

                        // Model Discovery Button & Results
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isDiscoveringModels = true
                                        discoveryMessage = null
                                        val models = aiClientService.discoverLocalModels(currentConfig.baseUrl, currentConfig.localProtocol)
                                        discoveredModels = models
                                        if (models.isNotEmpty()) {
                                            discoveryMessage = "Found ${models.size} local models"
                                        } else {
                                            discoveryMessage = "No models found at ${currentConfig.baseUrl}"
                                        }
                                        isDiscoveringModels = false
                                    }
                                },
                                enabled = !isDiscoveringModels
                            ) {
                                Text(if (isDiscoveringModels) "🔄 Discovering..." else "🔄 Discover Installed Models")
                            }
                            discoveryMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (discoveredModels.isNotEmpty()) {
                            Text("Discovered Models on Server:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                discoveredModels.take(4).forEach { model ->
                                    FilterChip(
                                        selected = currentConfig.primaryModelId == model,
                                        onClick = { updateCurrentConfig(currentConfig.copy(primaryModelId = model)) },
                                        label = { Text(model) }
                                    )
                                }
                            }
                        }

                        // Model Presets
                        Text("Model Presets / Catalog:", style = MaterialTheme.typography.labelMedium)
                        val localModels = aiCatalog.getModelsForProvider(AiProviderType.LOCAL_SERVER)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            localModels.forEach { modelEntry ->
                                FilterChip(
                                    selected = currentConfig.primaryModelId == modelEntry.id,
                                    onClick = { updateCurrentConfig(currentConfig.copy(primaryModelId = modelEntry.id)) },
                                    label = { Text(modelEntry.displayName) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = currentConfig.primaryModelId,
                            onValueChange = { updateCurrentConfig(currentConfig.copy(primaryModelId = it)) },
                            label = { Text("Active Model Identifier") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Connection Test Action & Indicator
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                connectionTestResult = aiClientService.testConnection(currentConfig)
                                isTestingConnection = false
                            }
                        },
                        enabled = !isTestingConnection
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Testing...")
                            } else {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Test Connection")
                            }
                        }
                    }

                    connectionTestResult?.let { res ->
                        val (bgColor, textColor, text) = if (res.isSuccess) {
                            Triple(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                "Connected (${res.latencyMs}ms) — Model ready"
                            )
                        } else {
                            Triple(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                                "Failed: ${res.errorMessage ?: "Unknown error"}"
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Global Preferences
        Text("Global AI Preferences:", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Auto-suggest tags on note creation", style = MaterialTheme.typography.bodyMedium)
                Text("Analyze note text immediately when created", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = autoSuggestTags,
                onCheckedChange = { autoSuggestTags = it }
            )
        }

        Column {
            Text("Max tags to generate per note: $maxTags", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = maxTags.toFloat(),
                onValueChange = { maxTags = it.toInt() },
                valueRange = 1f..10f,
                steps = 8
            )
        }

        // Bottom Save Button
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val updatedConfig = AiSettingsConfig(
                    activeProvider = activeProvider,
                    providers = providersMap,
                    autoSuggestOnNoteCreation = autoSuggestTags,
                    maxTagsToGenerate = maxTags
                )
                // 1. Save locally to device hardware storage
                deviceSettingsDriver.saveAiSettingsConfig(updatedConfig)

                // 2. Synchronize to user cloud config if authenticated
                val cloudCfg = authManager.currentCloudConfig
                if (cloudCfg != null) {
                    val element = runCatching {
                        json.encodeToJsonElement(AiSettingsConfig.serializer(), updatedConfig) as? JsonObject
                    }.getOrNull()
                    if (element != null) {
                        val updatedCloud = cloudCfg.copy(
                            moduleUserConfigs = cloudCfg.moduleUserConfigs + ("ai_settings" to element)
                        )
                        authManager.updateCloudConfig(updatedCloud)
                    }
                }
                aiSaveStatus = "AI Configuration saved successfully!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save AI Configuration")
        }
    }
}
