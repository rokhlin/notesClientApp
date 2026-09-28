package com.notes.client

import com.notes.client.auth.AuthManager
import com.notes.client.auth.SessionState
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.common.crypto.HmacSignatureEngine
import com.notes.common.crypto.ProtectedNoteCodec
import com.notes.common.models.DeviceLocalModuleConfig
import com.notes.common.models.Note
import com.notes.common.models.NoteType
import com.notes.common.models.StorageBackendType
import com.notes.common.models.StoragePathConfig
import com.notes.common.models.UserCloudConfig
import kotlin.test.*

class AuthSettingsAndProtectedNoteTest {

    @Test
    fun testAuthManagerInitialStateAndValidation() {
        val authManager = AuthManager()
        assertFalse(authManager.isAuthenticated)
        assertNull(authManager.currentProfile)
        assertNull(authManager.currentCloudConfig)
        assertTrue(authManager.sessionState.value is SessionState.Unauthenticated)

        // Invalid credentials rejection
        val blankEmailRes = authManager.login("", "secretpass123")
        assertTrue(blankEmailRes.isFailure)

        val shortPassRes = authManager.login("alice@company.org", "123")
        assertTrue(shortPassRes.isFailure)
    }

    @Test
    fun testAuthManagerLoginAndRegistrationGeneratesUniqueKeys() {
        val authManager = AuthManager()
        val registerRes = authManager.register("alice@company.org", "SecurePassword123!", "Alice Engineer")
        assertTrue(registerRes.isSuccess)
        val profile = registerRes.getOrThrow()

        assertTrue(authManager.isAuthenticated)
        assertEquals("alice@company.org", profile.email)
        assertEquals("Alice Engineer", profile.displayName)
        assertTrue(profile.userApiKey.startsWith("uak_"))
        assertTrue(profile.signingSecret.startsWith("sec_"))
        assertTrue(profile.userId.startsWith("usr_"))

        // Check cloud config initialization
        val config = authManager.currentCloudConfig
        assertNotNull(config)
        assertEquals(profile.userId, config.userId)
        assertEquals("alice@company.org", config.email)
        assertTrue(config.storagePaths.remoteStorageUrl?.contains(profile.userId) == true)

        // Logout
        authManager.logout()
        assertFalse(authManager.isAuthenticated)
        assertNull(authManager.currentProfile)
        assertNull(authManager.currentCloudConfig)
    }

    @Test
    fun testTwoTierConfigSeparationCloudVsDevice() {
        val authManager = AuthManager()
        authManager.login("bob@company.org", "ComplexPass987!")
        val profile = authManager.currentProfile!!

        // Tier 1: Cloud Configuration (per-user, synced)
        val initialConfig = authManager.currentCloudConfig!!
        val updatedConfig = initialConfig.copy(
            storagePaths = StoragePathConfig(
                localVaultPath = "custom_vault_bob",
                remoteStorageUrl = "https://r2.notesalltogether.com/users/${profile.userId}/data",
                storageBackendType = StorageBackendType.CLOUDFLARE_R2,
                autoSyncEnabled = true,
                syncIntervalSeconds = 45
            ),
            enabledModules = listOf("core-editor", "skia-canvas", "premium-tables")
        )
        val savedCloud = authManager.updateCloudConfig(updatedConfig)
        assertEquals("custom_vault_bob", savedCloud.storagePaths.localVaultPath)
        assertEquals(StorageBackendType.CLOUDFLARE_R2, savedCloud.storagePaths.storageBackendType)
        assertTrue(savedCloud.enabledModules.contains("premium-tables"))

        // Tier 2: Device-Local Runtime Settings (independent, physical device only)
        val deviceDriver = DeviceSettingsDriver()
        val defaultDevice = deviceDriver.getDeviceModuleConfig("skia-canvas")
        assertTrue(defaultDevice.isHardwareAccelerationEnabled)
        assertEquals(1.0f, defaultDevice.stylusPressureCurve)

        val updatedDevice = defaultDevice.copy(
            isHardwareAccelerationEnabled = false,
            stylusPressureCurve = 1.6f,
            localCacheDirectory = "device_cache/bob_skia"
        )
        deviceDriver.saveDeviceModuleConfig(updatedDevice)

        val retrievedDevice = deviceDriver.getDeviceModuleConfig("skia-canvas")
        assertFalse(retrievedDevice.isHardwareAccelerationEnabled)
        assertEquals(1.6f, retrievedDevice.stylusPressureCurve)
        assertEquals("device_cache/bob_skia", retrievedDevice.localCacheDirectory)

        // Logging out should NOT affect device-local hardware config
        authManager.logout()
        val postLogoutDevice = deviceDriver.getDeviceModuleConfig("skia-canvas")
        assertEquals(1.6f, postLogoutDevice.stylusPressureCurve)
    }

    @Test
    fun testProtectedNotePackagingAndUnpacking() {
        val originalMarkdown = "# Confidential Project Strategy\n\n- Sensitive milestone\n- Budget allocation"
        val password = "StrongNotePassword_42"

        val noteObject = ProtectedNoteCodec.createProtectedNote(
            noteId = "note_confidential_1",
            title = "Confidential Strategy",
            password = password,
            payloadContent = originalMarkdown,
            type = NoteType.TEXT,
            passwordHint = "Favorite number included"
        )

        val container = ProtectedNoteCodec.pack(noteObject)

        assertTrue(container.startsWith(ProtectedNoteCodec.MAGIC_HEADER))
        assertTrue(container.contains(ProtectedNoteCodec.PAYLOAD_BOUNDARY))
        // Verify payload is stored inside the container in native format (without ciphertext scrambling)
        assertTrue(container.contains("# Confidential Project Strategy"))

        // Unpack container
        val unpacked = ProtectedNoteCodec.unpack(container)
        assertEquals(originalMarkdown, unpacked.payloadContent)
        assertEquals("note_confidential_1", unpacked.metadata.noteId)
        assertEquals("Confidential Strategy", unpacked.metadata.title)
        assertEquals("Favorite number included", unpacked.metadata.passwordHint)

        // Password verification
        assertTrue(ProtectedNoteCodec.verifyPassword(password, unpacked.metadata))
        assertFalse(ProtectedNoteCodec.verifyPassword("WrongPassword", unpacked.metadata))
    }

    @Test
    fun testProtectedNoteRejectsTamperedHeaderOrSignature() {
        val raw = "# Top Secret"
        val noteObject = ProtectedNoteCodec.createProtectedNote(
            noteId = "note_tamper",
            title = "Top Secret",
            password = "correct_password",
            payloadContent = raw
        )
        val container = ProtectedNoteCodec.pack(noteObject)

        // Tamper magic header
        val tamperedHeader = container.replace(ProtectedNoteCodec.MAGIC_HEADER, "NA_CORRUPTED_V1")
        assertFailsWith<IllegalArgumentException> {
            ProtectedNoteCodec.unpack(tamperedHeader)
        }

        // Tamper application signature
        val tamperedSignature = container.replace("SIGNATURE:${ProtectedNoteCodec.APP_SIGNATURE}", "SIGNATURE:TAMPERED_SIG")
        assertFailsWith<IllegalArgumentException> {
            ProtectedNoteCodec.unpack(tamperedSignature)
        }
    }

    @Test
    fun testHmacCanonicalRequestSigningAndVerification() {
        val secret = "sec_test_secret_for_user_12345"
        val method = "PUT"
        val path = "/api/v1/user/config"
        val timestamp = System.currentTimeMillis()
        val nonce = "nonce_random_abc_123"
        val body = """{"userId":"usr_123","email":"test@notes.com"}"""
        val bodyHash = HmacSignatureEngine.computeBodyHash(body)

        val signature = HmacSignatureEngine.computeSignature(
            method = method,
            path = path,
            timestamp = timestamp,
            nonce = nonce,
            bodyHash = bodyHash,
            secret = secret
        )
        assertNotNull(signature)
        assertEquals(64, signature.length) // SHA-256 hex length

        // Verification passes with valid parameters
        val verifyOk = HmacSignatureEngine.verifySignature(
            method = method,
            path = path,
            timestamp = timestamp,
            nonce = nonce,
            bodyHash = bodyHash,
            secret = secret,
            expectedSignature = signature
        )
        assertTrue(verifyOk)

        // Tampered body hash must fail
        val tamperedBodyHash = HmacSignatureEngine.computeBodyHash("""{"userId":"usr_hacker"}""")
        val tamperedBodyVerify = HmacSignatureEngine.verifySignature(
            method = method,
            path = path,
            timestamp = timestamp,
            nonce = nonce,
            bodyHash = tamperedBodyHash,
            secret = secret,
            expectedSignature = signature
        )
        assertFalse(tamperedBodyVerify)

        // Wrong secret must fail
        val wrongSecretVerify = HmacSignatureEngine.verifySignature(
            method = method,
            path = path,
            timestamp = timestamp,
            nonce = nonce,
            bodyHash = bodyHash,
            secret = "sec_wrong_attacker_secret",
            expectedSignature = signature
        )
        assertFalse(wrongSecretVerify)
    }

    @Test
    fun testProtectedNoteFlagInNoteDataModel() {
        val note = Note(
            id = "note_prot_1",
            title = "Private Financials",
            content = "Sensitive spreadsheet data",
            type = NoteType.TEXT,
            isProtected = true
        )
        assertTrue(note.isProtected)
        assertFalse(note.isEncrypted)
    }
}
