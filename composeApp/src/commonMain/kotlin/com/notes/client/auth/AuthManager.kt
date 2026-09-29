package com.notes.client.auth

import com.notes.common.crypto.PureCrypto
import com.notes.common.crypto.PureCrypto.toHex
import com.notes.common.models.StorageBackendType
import com.notes.common.models.StoragePathConfig
import com.notes.common.models.UserAuthProfile
import com.notes.common.models.UserCloudConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.notes.client.util.currentTimeMillis

sealed interface SessionState {
    object Unauthenticated : SessionState
    object Authenticating : SessionState
    data class Authenticated(
        val profile: UserAuthProfile,
        val cloudConfig: UserCloudConfig
    ) : SessionState
}

/**
 * Universal client authentication and session state manager for NotesAlltogether.
 * Manages user credentials, per-user API key/secret, and user cloud configuration.
 */
class AuthManager(
    initialProfile: UserAuthProfile? = null,
    initialConfig: UserCloudConfig? = null
) {
    private val _sessionState = MutableStateFlow<SessionState>(
        if (initialProfile != null) {
            SessionState.Authenticated(
                profile = initialProfile,
                cloudConfig = initialConfig ?: defaultCloudConfig(initialProfile.userId, initialProfile.email)
            )
        } else {
            SessionState.Unauthenticated
        }
    )
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    val isAuthenticated: Boolean
        get() = _sessionState.value is SessionState.Authenticated

    val currentProfile: UserAuthProfile?
        get() = (_sessionState.value as? SessionState.Authenticated)?.profile

    val currentCloudConfig: UserCloudConfig?
        get() = (_sessionState.value as? SessionState.Authenticated)?.cloudConfig

    fun login(email: String, password: String): Result<UserAuthProfile> {
        if (email.isBlank() || password.length < 8) {
            return Result.failure(IllegalArgumentException("Invalid email or password must be >= 8 chars"))
        }

        val userId = "usr_" + PureCrypto.sha256(email.trim().lowercase().encodeToByteArray()).toHex().substring(0, 12)
        val userApiKey = "uak_" + PureCrypto.sha256((email + "_key").encodeToByteArray()).toHex().substring(0, 16)
        val signingSecret = "sec_" + PureCrypto.sha256((email + "_secret").encodeToByteArray()).toHex()

        val profile = UserAuthProfile(
            userId = userId,
            email = email.trim().lowercase(),
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            accessToken = "jwt_access_mock_token_for_$userId",
            refreshToken = "jwt_refresh_mock_token_for_$userId",
            userApiKey = userApiKey,
            signingSecret = signingSecret,
            expiresIn = 3600
        )

        val cloudConfig = defaultCloudConfig(userId, email)
        _sessionState.value = SessionState.Authenticated(profile, cloudConfig)
        return Result.success(profile)
    }

    fun register(email: String, password: String, displayName: String): Result<UserAuthProfile> {
        return login(email, password).map { profile ->
            if (displayName.isNotBlank()) {
                val updated = profile.copy(displayName = displayName)
                _sessionState.value = SessionState.Authenticated(updated, defaultCloudConfig(updated.userId, updated.email))
                updated
            } else {
                profile
            }
        }
    }

    fun logout() {
        _sessionState.value = SessionState.Unauthenticated
    }

    fun updateCloudConfig(newConfig: UserCloudConfig): UserCloudConfig {
        val state = _sessionState.value
        if (state is SessionState.Authenticated) {
            val updated = newConfig.copy(userId = state.profile.userId, updatedAt = currentTimeMillis())
            _sessionState.value = state.copy(cloudConfig = updated)
            return updated
        }
        return newConfig
    }

    companion object {
        fun defaultCloudConfig(userId: String, email: String): UserCloudConfig {
            return UserCloudConfig(
                userId = userId,
                email = email,
                displayName = email.substringBefore("@"),
                storagePaths = StoragePathConfig(
                    localVaultPath = "vault_$userId",
                    remoteStorageUrl = "https://r2.notesalltogether.com/users/$userId",
                    storageBackendType = StorageBackendType.NOTES_SERVER_SYNC,
                    autoSyncEnabled = true,
                    syncIntervalSeconds = 30
                ),
                enabledModules = listOf("core-editor", "skia-canvas"),
                updatedAt = currentTimeMillis()
            )
        }
    }
}
