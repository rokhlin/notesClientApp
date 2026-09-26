package com.notes.common.export.examples

import com.notes.common.models.cmn.*
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Production-ready sample demonstrating:
 * 1. Packing a .cmn container with CMN\x01 magic bytes and internal ZIP archive.
 * 2. Unpacking and validating a .cmn container.
 */
object CmnPackageManager {

    private val MAGIC_BYTES = byteArrayOf(0x43, 0x4D, 0x4E, 0x01) // "CMN\x01"
    private const val FORMAT_VERSION = 1
    private const val MIME_TYPE_STR = "application/x-notes-cmn"

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Packs document manifest, layers, and binary assets into a .cmn file.
     */
    fun packCmn(
        outputFile: File,
        manifest: CmnManifest,
        layers: Map<String, CmnLayerData>,
        assets: Map<String, ByteArray> = emptyMap()
    ) {
        FileOutputStream(outputFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                // 1. Write Custom Header Magic Bytes (4 bytes) + Version (4 bytes)
                bos.write(MAGIC_BYTES)
                val versionBuffer = ByteBuffer.allocate(4).putInt(FORMAT_VERSION).array()
                bos.write(versionBuffer)

                // 2. Stream ZIP entries
                ZipOutputStream(bos).use { zos ->
                    // A. mimetype entry (uncompressed STORED)
                    val mimeBytes = MIME_TYPE_STR.toByteArray(Charsets.US_ASCII)
                    val mimeEntry = ZipEntry("mimetype").apply {
                        method = ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                    }
                    zos.putNextEntry(mimeEntry)
                    zos.write(mimeBytes)
                    zos.closeEntry()

                    // B. manifest.json entry
                    val manifestJson = json.encodeToString(CmnManifest.serializer(), manifest)
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // C. Layer JSON entries
                    layers.forEach { (filename, layerData) ->
                        val layerPath = "layers/$filename"
                        val layerJson = json.encodeToString(CmnLayerData.serializer(), layerData)
                        zos.putNextEntry(ZipEntry(layerPath))
                        zos.write(layerJson.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }

                    // D. Binary assets (images/attachments)
                    assets.forEach { (assetName, assetData) ->
                        val assetPath = "assets/$assetName"
                        zos.putNextEntry(ZipEntry(assetPath))
                        zos.write(assetData)
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    /**
     * Unpacks and reads a .cmn file, verifying the header signature.
     */
    fun unpackCmn(inputFile: File): Pair<CmnManifest, Map<String, CmnLayerData>> {
        FileInputStream(inputFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                // 1. Verify Magic Bytes
                val magic = ByteArray(4)
                val readMagic = bis.read(magic)
                if (readMagic != 4 || !magic.contentEquals(MAGIC_BYTES)) {
                    throw IllegalArgumentException("Not a valid .cmn file: Missing CMN\\x01 signature.")
                }

                // 2. Read Version
                val versionBuf = ByteArray(4)
                bis.read(versionBuf)
                val version = ByteBuffer.wrap(versionBuf).int
                if (version > FORMAT_VERSION) {
                    throw UnsupportedOperationException("Unsupported .cmn format version: $version (max supported: $FORMAT_VERSION)")
                }

                // 3. Read internal ZIP entries
                var manifest: CmnManifest? = null
                val layers = mutableMapOf<String, CmnLayerData>()

                ZipInputStream(bis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "manifest.json" -> {
                                val content = zis.readBytes().toString(Charsets.UTF_8)
                                manifest = json.decodeFromString(CmnManifest.serializer(), content)
                            }
                            entry.name.startsWith("layers/") && entry.name.endsWith(".json") -> {
                                val content = zis.readBytes().toString(Charsets.UTF_8)
                                val layerData = json.decodeFromString(CmnLayerData.serializer(), content)
                                layers[entry.name] = layerData
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                requireNotNull(manifest) { "Corrupt .cmn package: manifest.json not found." }
                return Pair(manifest!!, layers)
            }
        }
    }
}
