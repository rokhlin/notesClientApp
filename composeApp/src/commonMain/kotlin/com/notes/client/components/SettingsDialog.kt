package com.notes.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.notes.client.auth.AuthManager
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.common.models.DeviceLocalModuleConfig
import com.notes.common.models.StorageBackendType
import com.notes.common.models.UserCloudConfig

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
                        Text(
                            text = "⚙️ System Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
                        text = { Text("☁️ Cloud Profile & Storage") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("💻 Device & Hardware Settings") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
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
                                    Text("🗄️ Inspect Sandboxed Vault & Index")
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
                                    cloudSaveStatus = "✅ Cloud configuration saved successfully!"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Cloud Profile & Paths")
                            }
                        }
                    } else {
                        // TIER 2: DEVICE LOCAL RUNTIME SETTINGS
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🔒 Hardware settings are stored only on this physical device and never uploaded to cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

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
                                    deviceSaveStatus = "✅ Device settings saved to local hardware storage!"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Device Hardware Settings")
                            }
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
