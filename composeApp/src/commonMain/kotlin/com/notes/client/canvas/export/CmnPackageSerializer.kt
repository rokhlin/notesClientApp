package com.notes.client.canvas.export

import com.notes.common.models.CmnManifest
import kotlinx.serialization.json.Json

/**
 * Serializer and deserializer for the .cmn (Custom Multi-layer Note) compound package format.
 * Adheres to ADR Q15, Q16, and cmn-file-format skill.
 *
 * Dedicated magic header bytes: 0x43 0x4D 0x4E 0x01 ("CMN\x01").
 */
object CmnPackageSerializer {

    val MAGIC_HEADER = byteArrayOf(0x43.toByte(), 0x4D.toByte(), 0x4E.toByte(), 0x01.toByte())

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Checks if the byte array starts with the dedicated CMN\x01 magic header.
     */
    fun isValidCmnHeader(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_HEADER.size) return false
        for (i in MAGIC_HEADER.indices) {
            if (bytes[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    /**
     * Serializes a CmnManifest document into a .cmn binary payload with the 4-byte magic header.
     */
    fun serialize(manifest: CmnManifest): ByteArray {
        val jsonText = json.encodeToString(CmnManifest.serializer(), manifest)
        val jsonBytes = jsonText.encodeToByteArray()
        val result = ByteArray(MAGIC_HEADER.size + jsonBytes.size)
        MAGIC_HEADER.copyInto(result, destinationOffset = 0)
        jsonBytes.copyInto(result, destinationOffset = MAGIC_HEADER.size)
        return result
    }

    /**
     * Deserializes a .cmn binary payload, verifying the magic header and decoding the JSON manifest.
     * Throws IllegalArgumentException if magic header is missing or corrupted.
     */
    fun deserialize(bytes: ByteArray): CmnManifest {
        if (!isValidCmnHeader(bytes)) {
            throw IllegalArgumentException("Invalid CMN magic header")
        }
        val jsonBytes = bytes.copyOfRange(MAGIC_HEADER.size, bytes.size)
        val jsonText = jsonBytes.decodeToString()
        return try {
            json.decodeFromString(CmnManifest.serializer(), jsonText)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted CMN payload: ${e.message}", e)
        }
    }
}
