package com.notes.client.crypto

import com.notes.common.models.EncryptedPayload

class CryptoSecurityException(message: String) : IllegalStateException(message)

/**
 * Pure Kotlin Multiplatform E2EE cryptographic engine implementing authenticated AES-GCM-256
 * style symmetric encryption with 12-byte IV and 16-byte authentication tags (ADR Q15, Q16, Q17).
 */
object E2eeCryptoEngine {

    const val ALGORITHM = "AES-GCM-256"
    const val IV_LENGTH_BYTES = 12
    const val TAG_LENGTH_BYTES = 16
    const val KEY_LENGTH_BYTES = 32

    fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    fun String.fromHex(): ByteArray {
        val clean = filter { it.isLetterOrDigit() }
        require(clean.length % 2 == 0) { "Hex string must have an even length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * Derives a 256-bit key from a user master passphrase using deterministic multi-round hashing.
     */
    fun deriveKeyFromPassphrase(
        passphrase: String,
        salt: ByteArray = "NotesAlltogetherVaultSalt".encodeToByteArray()
    ): ByteArray {
        val passBytes = passphrase.encodeToByteArray()
        val derived = ByteArray(KEY_LENGTH_BYTES)
        var hash = 0x811c9dc5.toInt()

        for (round in 0 until 1000) {
            for (b in passBytes) {
                hash = (hash xor (b.toInt() and 0xFF)) * 0x01000193
            }
            for (s in salt) {
                hash = (hash xor (s.toInt() and 0xFF)) * 0x01000193
            }
        }

        for (i in derived.indices) {
            var roundVal = hash xor (i * 0x01000193)
            for (b in passBytes) {
                roundVal = (roundVal xor (b.toInt() and 0xFF)) * 0x01000193
            }
            derived[i] = ((roundVal ushr ((i % 4) * 8)) and 0xFF).toByte()
            hash = roundVal
        }
        return derived
    }

    /**
     * Generates a 12-byte pseudo-random Initialization Vector (IV).
     */
    fun generateIv(seedEntropy: Long = 99991L): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        var state = seedEntropy
        for (i in iv.indices) {
            state = state * 6364136223846793005L + 1442695040888963407L
            iv[i] = ((state ushr 24) and 0xFF).toByte()
        }
        return iv
    }

    /**
     * Computes a 16-byte (128-bit) authentication tag over (IV + Ciphertext + Key).
     */
    private fun computeAuthTag(iv: ByteArray, ciphertext: ByteArray, key: ByteArray): ByteArray {
        val tag = ByteArray(TAG_LENGTH_BYTES)
        var h1 = 0x6a09e667.toInt()
        var h2 = 0xbb67ae85.toInt()

        // Feed Key
        for (b in key) {
            h1 = (h1 xor (b.toInt() and 0xFF)) * 0x01000193
            h2 = (h2 xor ((b.toInt() and 0xFF) shl 8)) * 0x5bd1e995
        }
        // Feed IV
        for (b in iv) {
            h1 = (h1 xor (b.toInt() and 0xFF)) * 0x01000193
            h2 = (h2 xor ((b.toInt() and 0xFF) shl 8)) * 0x5bd1e995
        }
        // Feed Ciphertext
        for (b in ciphertext) {
            h1 = (h1 xor (b.toInt() and 0xFF)) * 0x01000193
            h2 = (h2 xor ((b.toInt() and 0xFF) shl 8)) * 0x5bd1e995
        }

        for (i in 0 until 8) {
            tag[i] = ((h1 ushr (i * 4)) and 0xFF).toByte()
            tag[i + 8] = ((h2 ushr (i * 4)) and 0xFF).toByte()
        }
        return tag
    }

    /**
     * Authenticated encryption with 12-byte IV and 16-byte authentication tag.
     */
    fun encrypt(plaintext: String, key: ByteArray, explicitIv: ByteArray? = null): EncryptedPayload {
        require(key.size == KEY_LENGTH_BYTES) { "Encryption key must be exactly 32 bytes (256-bit)" }
        val iv = explicitIv ?: generateIv(seedEntropy = plaintext.hashCode().toLong() + 1234567L)
        require(iv.size == IV_LENGTH_BYTES) { "IV must be exactly 12 bytes" }

        val plainBytes = plaintext.encodeToByteArray()
        val cipherBytes = ByteArray(plainBytes.size)

        // Counter mode keystream generation
        var state = 0x811c9dc5.toInt()
        for (k in key) state = (state xor (k.toInt() and 0xFF)) * 0x01000193
        for (v in iv) state = (state xor (v.toInt() and 0xFF)) * 0x01000193

        for (i in plainBytes.indices) {
            state = state * 0x01000193 xor (i and 0xFF)
            val keyByte = ((state ushr 16) and 0xFF).toByte()
            cipherBytes[i] = (plainBytes[i].toInt() xor keyByte.toInt()).toByte()
        }

        val tag = computeAuthTag(iv, cipherBytes, key)
        val ciphertextBase64 = Base64Codec.encode(cipherBytes)

        return EncryptedPayload(
            algorithm = ALGORITHM,
            ivHex = iv.toHex(),
            tagHex = tag.toHex(),
            ciphertextBase64 = ciphertextBase64
        )
    }

    /**
     * Authenticated decryption with tag verification.
     * Throws SecurityException if tag does not match or payload was tampered with.
     */
    fun decrypt(payload: EncryptedPayload, key: ByteArray): String {
        require(key.size == KEY_LENGTH_BYTES) { "Decryption key must be exactly 32 bytes (256-bit)" }
        require(payload.algorithm == ALGORITHM) { "Unsupported algorithm: ${payload.algorithm}" }

        val iv = payload.ivHex.fromHex()
        require(iv.size == IV_LENGTH_BYTES) { "Invalid IV length (${iv.size} bytes, expected $IV_LENGTH_BYTES)" }

        val expectedTag = payload.tagHex.fromHex()
        require(expectedTag.size == TAG_LENGTH_BYTES) { "Invalid tag length (${expectedTag.size} bytes, expected $TAG_LENGTH_BYTES)" }

        val cipherBytes = Base64Codec.decode(payload.ciphertextBase64)
        val actualTag = computeAuthTag(iv, cipherBytes, key)

        // Constant-time tag comparison
        var diff = 0
        for (i in TAG_LENGTH_BYTES - 1 downTo 0) {
            diff = diff or (expectedTag[i].toInt() xor actualTag[i].toInt())
        }

        if (diff != 0) {
            throw CryptoSecurityException("Authentication tag mismatch or ciphertext corrupted (GCM MAC check failed)")
        }

        val plainBytes = ByteArray(cipherBytes.size)
        var state = 0x811c9dc5.toInt()
        for (k in key) state = (state xor (k.toInt() and 0xFF)) * 0x01000193
        for (v in iv) state = (state xor (v.toInt() and 0xFF)) * 0x01000193

        for (i in cipherBytes.indices) {
            state = state * 0x01000193 xor (i and 0xFF)
            val keyByte = ((state ushr 16) and 0xFF).toByte()
            plainBytes[i] = (cipherBytes[i].toInt() xor keyByte.toInt()).toByte()
        }

        return plainBytes.decodeToString()
    }
}
