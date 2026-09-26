package com.notes.client.storage

import com.notes.common.models.Note
import com.notes.common.models.NoteMetadata
import com.notes.common.models.NotesIndexCatalog

/**
 * Summary metrics of the local sandboxed vault storage.
 */
data class VaultStats(
    val totalNotes: Int,
    val textNotes: Int,
    val canvasNotes: Int,
    val encryptedNotes: Int,
    val totalSizeBytes: Long,
    val catalogVersion: Int
)

/**
 * Storage driver abstraction for cross-platform file sandboxing (Android, iOS, Desktop JVM, WasmJs).
 */
interface StorageDriver {
    fun readText(path: String): String?
    fun writeText(path: String, content: String)
    fun delete(path: String): Boolean
    fun listFiles(): List<String>
    fun exists(path: String): Boolean
}

/**
 * Contract for local note persistence using lightweight JSON catalog indexing (ADR Q17, Q19, Q29).
 */
interface NoteStorageRepository {
    /**
     * Retrieves the current in-memory notes index catalog without disk reading.
     */
    fun getCatalog(): NotesIndexCatalog

    /**
     * High-speed in-memory title and tag query evaluation without reading note bodies (ADR Q17).
     */
    fun searchNotes(query: String): List<NoteMetadata>

    /**
     * Filters notes strictly matching a specific tag.
     */
    fun filterByTag(tag: String): List<NoteMetadata>

    /**
     * Loads the full note payload (content/body) from its decoupled individual file.
     */
    fun loadNote(id: String): Note?

    /**
     * Saves a note, storing its payload in an isolated file and updating the title-only catalog.
     */
    fun saveNote(note: Note): NoteMetadata

    /**
     * Deletes a note's payload file and purges its metadata from the catalog.
     */
    fun deleteNote(id: String): Boolean

    /**
     * Re-scans all note files in the sandboxed repository and regenerates `notes_index.json`.
     */
    fun rebuildCatalog(): NotesIndexCatalog

    /**
     * Computes real-time statistics of the storage vault.
     */
    fun getVaultStats(): VaultStats
}
