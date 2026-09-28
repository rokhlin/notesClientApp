package com.notes.common.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class StorageBackendType {
    LOCAL_DISK,
    CLOUDFLARE_R2,
    MINIO_S3,
    NOTES_SERVER_SYNC
}

@Serializable
data class StoragePathConfig(
    val localVaultPath: String = "default_vault",
    val remoteStorageUrl: String? = null,
    val storageBackendType: StorageBackendType = StorageBackendType.NOTES_SERVER_SYNC,
    val autoSyncEnabled: Boolean = false,
    val syncIntervalSeconds: Int = 30
)

/**
 * Tier 1: User-Specific Cloud Profile.
 * Synchronized with backend user account and stored per user.
 */
@Serializable
data class UserCloudConfig(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val storagePaths: StoragePathConfig = StoragePathConfig(),
    val enabledModules: List<String> = listOf("core-editor", "skia-canvas"),
    val moduleLicenses: Map<String, String> = emptyMap(), // moduleId -> licenseToken
    val moduleUserConfigs: Map<String, JsonObject> = emptyMap(), // moduleId -> user settings JSON
    val updatedAt: Long = 0L
)

/**
 * Tier 2: Device-Local Module Runtime Settings.
 * Stored strictly on physical device based on module presence, independent of user cloud sync.
 */
@Serializable
data class DeviceLocalModuleConfig(
    val moduleId: String,
    val isHardwareAccelerationEnabled: Boolean = true,
    val stylusPressureCurve: Float = 1.0f,
    val localCacheDirectory: String = "",
    val maxLocalCacheBytes: Long = 524_288_000L, // 500 MB
    val deviceDensityScale: Float = 1.0f,
    val lastUpdated: Long = 0L
)
