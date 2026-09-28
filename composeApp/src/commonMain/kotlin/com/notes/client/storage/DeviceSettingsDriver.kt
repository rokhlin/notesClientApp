package com.notes.client.storage

import com.notes.common.models.DeviceLocalModuleConfig
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Driver for Tier 2 Device-Local Module Runtime Settings.
 * Stored strictly on the local device based on module presence, independent of user cloud sync.
 */
class DeviceSettingsDriver(
    private val storageDriver: StorageDriver = InMemoryStorageDriver()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val cachedConfigs = ConcurrentHashMap<String, DeviceLocalModuleConfig>()

    companion object {
        fun moduleConfigPath(moduleId: String): String = "device_modules/module_$moduleId.json"
    }

    fun getDeviceModuleConfig(moduleId: String): DeviceLocalModuleConfig {
        return cachedConfigs.computeIfAbsent(moduleId) { id ->
            val path = moduleConfigPath(id)
            val text = storageDriver.readText(path)
            if (!text.isNullOrBlank()) {
                runCatching {
                    json.decodeFromString(DeviceLocalModuleConfig.serializer(), text)
                }.getOrElse {
                    createDefault(id)
                }
            } else {
                createDefault(id)
            }
        }
    }

    fun saveDeviceModuleConfig(config: DeviceLocalModuleConfig) {
        val updated = config.copy(lastUpdated = System.currentTimeMillis())
        cachedConfigs[config.moduleId] = updated
        val path = moduleConfigPath(config.moduleId)
        val text = json.encodeToString(DeviceLocalModuleConfig.serializer(), updated)
        storageDriver.writeText(path, text)
    }

    private fun createDefault(moduleId: String): DeviceLocalModuleConfig {
        return DeviceLocalModuleConfig(
            moduleId = moduleId,
            isHardwareAccelerationEnabled = true,
            stylusPressureCurve = 1.0f,
            localCacheDirectory = "cache/$moduleId",
            maxLocalCacheBytes = 524_288_000L,
            deviceDensityScale = 1.0f,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
