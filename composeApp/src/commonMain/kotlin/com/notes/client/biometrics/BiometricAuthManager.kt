package com.notes.client.biometrics

/**
 * Hardware biometric sensor status.
 */
enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricAuthResult {
    data class Success(val key: ByteArray) : BiometricAuthResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return key.contentEquals(other.key)
        }
        override fun hashCode(): Int = key.contentHashCode()
    }
    data class Failure(val reason: String) : BiometricAuthResult()
    object Cancelled : BiometricAuthResult()
}

/**
 * Contract for platform biometric authentication and hardware key release (ADR Q16).
 */
interface BiometricAuthManager {
    /**
     * Checks if the device has biometric sensors and enrolled credentials.
     */
    fun canAuthenticate(): BiometricStatus

    /**
     * User preference toggle state for biometric unlocking.
     */
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)

    /**
     * Retains the 256-bit vault key in hardware enclave / keystore.
     */
    fun storeVaultKey(key: ByteArray): Boolean
    fun getStoredVaultKey(): ByteArray?
    fun clearStoredKey(): Boolean

    /**
     * Prompts the user with biometric sensor scan (Fingerprint, Touch ID, Face ID).
     */
    fun authenticate(
        title: String = "Unlock Vault",
        subtitle: String = "Scan biometric credentials to decrypt notes",
        onResult: (BiometricAuthResult) -> Unit
    )
}
