package com.notes.client.storage

import com.notes.common.models.Note
import com.notes.common.models.NoteMetadata
import com.notes.common.models.NoteType
import com.notes.common.models.NotesIndexCatalog
import kotlinx.serialization.json.Json

/**
 * Default in-memory sandboxed storage driver for multiplatform runtime safety.
 */
class InMemoryStorageDriver(initialFiles: Map<String, String> = emptyMap()) : StorageDriver {
    private val files = initialFiles.toMutableMap()

    override fun readText(path: String): String? = files[path]

    override fun writeText(path: String, content: String) {
        files[path] = content
    }

    override fun delete(path: String): Boolean {
        return files.remove(path) != null
    }

    override fun listFiles(): List<String> = files.keys.toList()

    override fun exists(path: String): Boolean = files.containsKey(path)
}

/**
 * Pure Kotlin lightweight JSON index repository implementing ADR Q17, Q19, and Q29.
 *
 * Persists note metadata in `notes_index.json` (title-only, unencrypted search indexing)
 * and keeps full note bodies in decoupled isolated files under `notes/note_<id>.json`.
 */
class JsonIndexNoteRepository(
    private val driver: StorageDriver = InMemoryStorageDriver()
) : NoteStorageRepository {

    companion object {
        const val CATALOG_FILE = "notes_index.json"
        fun noteFilePath(id: String): String = "notes/note_$id.json"
    }

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var cachedCatalog: NotesIndexCatalog

    init {
        cachedCatalog = loadCatalogFromDisk()
    }

    private fun loadCatalogFromDisk(): NotesIndexCatalog {
        val catalogJson = driver.readText(CATALOG_FILE)
        return if (!catalogJson.isNullOrBlank()) {
            try {
                json.decodeFromString(NotesIndexCatalog.serializer(), catalogJson)
            } catch (e: Exception) {
                // If corrupted, fallback to clean catalog
                NotesIndexCatalog(version = 1, lastSyncedAt = 0L, notes = emptyList())
            }
        } else {
            NotesIndexCatalog(version = 1, lastSyncedAt = 0L, notes = emptyList())
        }
    }

    private fun persistCatalog(catalog: NotesIndexCatalog) {
        cachedCatalog = catalog
        val catalogJson = json.encodeToString(NotesIndexCatalog.serializer(), catalog)
        driver.writeText(CATALOG_FILE, catalogJson)
    }

    override fun getCatalog(): NotesIndexCatalog = cachedCatalog

    override fun searchNotes(query: String): List<NoteMetadata> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return cachedCatalog.notes

        return cachedCatalog.notes.filter { meta ->
            meta.title.contains(trimmed, ignoreCase = true) ||
                    meta.tags.any { tag -> tag.contains(trimmed, ignoreCase = true) }
        }
    }

    override fun filterByTag(tag: String): List<NoteMetadata> {
        val cleanTag = tag.trim().removePrefix("#")
        return cachedCatalog.notes.filter { meta ->
            meta.tags.any { it.equals(cleanTag, ignoreCase = true) }
        }
    }

    override fun loadNote(id: String): Note? {
        val path = noteFilePath(id)
        val noteContent = driver.readText(path) ?: return null
        return try {
            json.decodeFromString(Note.serializer(), noteContent)
        } catch (e: Exception) {
            null
        }
    }

    override fun saveNote(note: Note): NoteMetadata {
        val noteJson = json.encodeToString(Note.serializer(), note)
        val path = noteFilePath(note.id)
        driver.writeText(path, noteJson)

        val sizeBytes = noteJson.encodeToByteArray().size.toLong()
        val updatedAt = if (note.updatedAt > 0L) note.updatedAt else note.createdAt

        val metadata = NoteMetadata(
            id = note.id,
            title = note.title,
            tags = note.tags,
            type = note.type,
            isEncrypted = note.isEncrypted,
            updatedAt = updatedAt,
            sizeBytes = sizeBytes
        )

        val existingNotes = cachedCatalog.notes.toMutableList()
        val existingIndex = existingNotes.indexOfFirst { it.id == note.id }
        if (existingIndex >= 0) {
            existingNotes[existingIndex] = metadata
        } else {
            existingNotes.add(0, metadata)
        }

        persistCatalog(
            cachedCatalog.copy(
                notes = existingNotes,
                lastSyncedAt = updatedAt
            )
        )

        return metadata
    }

    override fun deleteNote(id: String): Boolean {
        val path = noteFilePath(id)
        val removedFile = driver.delete(path)
        val updatedNotes = cachedCatalog.notes.filterNot { it.id == id }
        val changed = updatedNotes.size != cachedCatalog.notes.size

        if (changed || removedFile) {
            persistCatalog(cachedCatalog.copy(notes = updatedNotes))
            return true
        }
        return false
    }

    override fun rebuildCatalog(): NotesIndexCatalog {
        val noteFiles = driver.listFiles().filter {
            it.startsWith("notes/note_") && it.endsWith(".json")
        }

        val reindexed = mutableListOf<NoteMetadata>()
        for (file in noteFiles) {
            val content = driver.readText(file) ?: continue
            try {
                val note = json.decodeFromString(Note.serializer(), content)
                val sizeBytes = content.encodeToByteArray().size.toLong()
                reindexed.add(
                    NoteMetadata(
                        id = note.id,
                        title = note.title,
                        tags = note.tags,
                        type = note.type,
                        isEncrypted = note.isEncrypted,
                        updatedAt = if (note.updatedAt > 0L) note.updatedAt else note.createdAt,
                        sizeBytes = sizeBytes
                    )
                )
            } catch (_: Exception) {
                // Ignore malformed files
            }
        }

        val sorted = reindexed.sortedByDescending { it.updatedAt }
        val newCatalog = NotesIndexCatalog(
            version = 1,
            lastSyncedAt = 0L,
            notes = sorted
        )
        persistCatalog(newCatalog)
        return newCatalog
    }

    override fun getVaultStats(): VaultStats {
        val notes = cachedCatalog.notes
        return VaultStats(
            totalNotes = notes.size,
            textNotes = notes.count { it.type == NoteType.TEXT },
            canvasNotes = notes.count { it.type == NoteType.CANVAS },
            encryptedNotes = notes.count { it.isEncrypted },
            totalSizeBytes = notes.sumOf { it.sizeBytes },
            catalogVersion = cachedCatalog.version
        )
    }
}
