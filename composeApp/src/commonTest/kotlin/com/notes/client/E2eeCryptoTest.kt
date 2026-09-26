package com.notes.client

import com.notes.client.crypto.Base64Codec
import com.notes.client.crypto.Bip39RecoveryKit
import com.notes.client.crypto.CryptoSecurityException
import com.notes.client.crypto.E2eeCryptoEngine
import kotlin.test.*

class E2eeCryptoTest {

    @Test
    fun testBase64CodecRoundtrip() {
        val sampleBytes = "Hello Zero-Knowledge E2EE World!".encodeToByteArray()
        val encoded = Base64Codec.encode(sampleBytes)
        val decoded = Base64Codec.decode(encoded)
        assertContentEquals(sampleBytes, decoded)
    }

    @Test
    fun testBip39MnemonicGenerationAndValidation() {
        val mnemonic = Bip39RecoveryKit.generateMnemonic(12)
        assertEquals(12, mnemonic.size)
        assertTrue(Bip39RecoveryKit.isValidMnemonic(mnemonic))

        // Invalid count
        assertFalse(Bip39RecoveryKit.isValidMnemonic(mnemonic.take(11)))

        // Invalid word
        val corrupted = mnemonic.toMutableList()
        corrupted[0] = "not_in_bip39_dictionary_xyz"
        assertFalse(Bip39RecoveryKit.isValidMnemonic(corrupted))
    }

    @Test
    fun testBip39DeterministicKeyDerivation() {
        val words = Bip39RecoveryKit.generateMnemonic(12, seedEntropy = 12345L)
        val key1 = Bip39RecoveryKit.deriveKeyFromMnemonic(words)
        val key2 = Bip39RecoveryKit.deriveKeyFromMnemonic(words)

        assertEquals(32, key1.size, "Derived root key must be 32 bytes (256-bit)")
        assertContentEquals(key1, key2, "Mnemonic key derivation must be completely deterministic")

        val differentWords = Bip39RecoveryKit.generateMnemonic(12, seedEntropy = 99999L)
        val differentKey = Bip39RecoveryKit.deriveKeyFromMnemonic(differentWords)
        assertFalse(key1.contentEquals(differentKey), "Different seed phrases must produce different keys")
    }

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val passphrase = "SuperSecretMasterPassphrase123!"
        val key = E2eeCryptoEngine.deriveKeyFromPassphrase(passphrase)
        assertEquals(32, key.size)

        val plaintext = "# Architecture Vault Spec\n\nConfidential client notes encrypted with AES-GCM-256."
        val payload = E2eeCryptoEngine.encrypt(plaintext, key)

        assertEquals("AES-GCM-256", payload.algorithm)
        assertEquals(24, payload.ivHex.length, "12-byte IV must be 24 hex chars")
        assertEquals(32, payload.tagHex.length, "16-byte Tag must be 32 hex chars")
        assertTrue(payload.ciphertextBase64.isNotEmpty())

        val decrypted = E2eeCryptoEngine.decrypt(payload, key)
        assertEquals(plaintext, decrypted, "Decrypted text must match original plaintext")
    }

    @Test
    fun testTamperedCiphertextRejection() {
        val key = E2eeCryptoEngine.deriveKeyFromPassphrase("MasterPass123")
        val plaintext = "Top secret payload data"
        val payload = E2eeCryptoEngine.encrypt(plaintext, key)

        // Mutate last character of ciphertext
        val tamperedCipher = if (payload.ciphertextBase64.endsWith("A")) {
            payload.ciphertextBase64.dropLast(1) + "B"
        } else {
            payload.ciphertextBase64.dropLast(1) + "A"
        }
        val tamperedPayload = payload.copy(ciphertextBase64 = tamperedCipher)

        assertFailsWith<CryptoSecurityException> {
            E2eeCryptoEngine.decrypt(tamperedPayload, key)
        }
    }

    @Test
    fun testTamperedAuthTagRejection() {
        val key = E2eeCryptoEngine.deriveKeyFromPassphrase("MasterPass123")
        val payload = E2eeCryptoEngine.encrypt("Note content", key)

        // Flip first byte in authentication tag
        val firstChar = payload.tagHex[0]
        val flipped = if (firstChar == 'a') 'b' else 'a'
        val tamperedTag = flipped + payload.tagHex.substring(1)
        val tamperedPayload = payload.copy(tagHex = tamperedTag)

        assertFailsWith<CryptoSecurityException> {
            E2eeCryptoEngine.decrypt(tamperedPayload, key)
        }
    }

    @Test
    fun testWrongKeyRejection() {
        val keyA = E2eeCryptoEngine.deriveKeyFromPassphrase("CorrectPassphrase")
        val keyB = E2eeCryptoEngine.deriveKeyFromPassphrase("WrongPassphrase")

        val payload = E2eeCryptoEngine.encrypt("Highly private message", keyA)

        assertFailsWith<CryptoSecurityException> {
            E2eeCryptoEngine.decrypt(payload, keyB)
        }
    }
}

