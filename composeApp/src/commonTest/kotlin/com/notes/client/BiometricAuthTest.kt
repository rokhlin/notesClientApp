package com.notes.client

import com.notes.client.biometrics.BiometricAuthResult
import com.notes.client.biometrics.BiometricStatus
import com.notes.client.biometrics.SimulatedBiometricAuthManager
import kotlin.test.*

class BiometricAuthTest {

    private lateinit var manager: SimulatedBiometricAuthManager

    @BeforeTest
    fun setup() {
        manager = SimulatedBiometricAuthManager()
    }

    @Test
    fun testBiometricStatusReporting() {
        assertEquals(BiometricStatus.AVAILABLE, manager.canAuthenticate())

        manager.setStatus(BiometricStatus.NOT_ENROLLED)
        assertEquals(BiometricStatus.NOT_ENROLLED, manager.canAuthenticate())

        manager.setStatus(BiometricStatus.NO_HARDWARE)
        assertEquals(BiometricStatus.NO_HARDWARE, manager.canAuthenticate())

        manager.setStatus(BiometricStatus.UNAVAILABLE)
        assertEquals(BiometricStatus.UNAVAILABLE, manager.canAuthenticate())
    }

    @Test
    fun testStoreAndRetrieveVaultKey() {
        val sampleKey = ByteArray(32) { (it + 42).toByte() }
        assertTrue(manager.storeVaultKey(sampleKey))

        val retrieved = manager.getStoredVaultKey()
        assertNotNull(retrieved)
        assertContentEquals(sampleKey, retrieved)

        assertTrue(manager.clearStoredKey())
        assertNull(manager.getStoredVaultKey())
    }

    @Test
    fun testSuccessfulAuthenticationReleasesKey() {
        val expectedKey = ByteArray(32) { (it * 3).toByte() }
        manager.storeVaultKey(expectedKey)
        manager.shouldSucceedNextAuth = true

        var receivedResult: BiometricAuthResult? = null
        manager.authenticate { result ->
            receivedResult = result
        }

        assertNotNull(receivedResult)
        assertTrue(receivedResult is BiometricAuthResult.Success)
        assertContentEquals(expectedKey, (receivedResult as BiometricAuthResult.Success).key)
    }

    @Test
    fun testFailureAuthentication() {
        manager.shouldSucceedNextAuth = false

        var receivedResult: BiometricAuthResult? = null
        manager.authenticate { result ->
            receivedResult = result
        }

        assertNotNull(receivedResult)
        assertTrue(receivedResult is BiometricAuthResult.Failure)
        assertTrue((receivedResult as BiometricAuthResult.Failure).reason.contains("Biometric verification failed"))
    }

    @Test
    fun testDisabledBiometricsRejectsAuth() {
        manager.setBiometricEnabled(false)

        var receivedResult: BiometricAuthResult? = null
        manager.authenticate { result ->
            receivedResult = result
        }

        assertNotNull(receivedResult)
        assertTrue(receivedResult is BiometricAuthResult.Failure)
        assertTrue((receivedResult as BiometricAuthResult.Failure).reason.contains("disabled"))
    }
}
