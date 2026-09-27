package com.notes.client.biometrics

/**
 * Cross-platform simulated BiometricAuthManager with configurable sensor states and secure key memory container.
 */
class SimulatedBiometricAuthManager(
    private var status: BiometricStatus = BiometricStatus.AVAILABLE,
    private var isEnabled: Boolean = true,
    initialKey: ByteArray? = null
) : BiometricAuthManager {

    private var storedKey: ByteArray? = initialKey?.copyOf()
    var shouldSucceedNextAuth: Boolean = true

    override fun canAuthenticate(): BiometricStatus = status

    fun setStatus(newStatus: BiometricStatus) {
        status = newStatus
    }

    override fun isBiometricEnabled(): Boolean = isEnabled

    override fun setBiometricEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    override fun storeVaultKey(key: ByteArray): Boolean {
        storedKey = key.copyOf()
        return true
    }

    override fun getStoredVaultKey(): ByteArray? = storedKey?.copyOf()

    override fun clearStoredKey(): Boolean {
        storedKey = null
        return true
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        if (!isEnabled) {
            onResult(BiometricAuthResult.Failure("Biometric authentication is disabled in vault settings"))
            return
        }

        when (status) {
            BiometricStatus.NO_HARDWARE -> {
                onResult(BiometricAuthResult.Failure("No biometric hardware detected on this device"))
                return
            }
            BiometricStatus.NOT_ENROLLED -> {
                onResult(BiometricAuthResult.Failure("No biometric credentials enrolled in system settings"))
                return
            }
            BiometricStatus.UNAVAILABLE -> {
                onResult(BiometricAuthResult.Failure("Biometric sensor is temporarily unavailable"))
                return
            }
            BiometricStatus.AVAILABLE -> {
                if (shouldSucceedNextAuth) {
                    val key = storedKey ?: ByteArray(32) { (it * 7).toByte() }
                    onResult(BiometricAuthResult.Success(key))
                } else {
                    onResult(BiometricAuthResult.Failure("Biometric verification failed: Sensor unrecognized"))
                }
            }
        }
    }
}
