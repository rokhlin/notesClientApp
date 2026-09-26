package com.notes.client

import com.notes.client.storage.InMemoryStorageDriver
import com.notes.client.storage.JsonIndexNoteRepository
import com.notes.common.models.Note
import com.notes.common.models.NoteType
import kotlin.test.*

class JsonIndexStorageTest {

    private lateinit var driver: InMemoryStorageDriver
    private lateinit var repository: JsonIndexNoteRepository

    @BeforeTest
    fun setup() {
        driver = InMemoryStorageDriver()
        repository = JsonIndexNoteRepository(driver)
    }

    @Test
    fun testSaveNoteCreatesDecoupledPayloadAndTitleOnlyCatalog() {
        val note = Note(
            id = "note_101",
            title = "Secret Vault Architecture",
            content = "# Highly confidential note body text that should never be in the index\n\nSome sensitive details.",
            type = NoteType.TEXT,
            tags = listOf("architecture", "vault"),
            createdAt = 1717030000000L
        )

        val metadata = repository.saveNote(note)
        assertEquals("note_101", metadata.id)
        assertEquals("Secret Vault Architecture", metadata.title)
        assertTrue(metadata.sizeBytes > 0)

        // 1. Verify decoupled file exists in driver
        val noteFilePath = JsonIndexNoteRepository.noteFilePath("note_101")
        assertTrue(driver.exists(noteFilePath), "Note file must exist in storage")
        val savedRawNote = driver.readText(noteFilePath)
        assertNotNull(savedRawNote)
        assertTrue(savedRawNote.contains("Highly confidential note body text"))

        // 2. Verify notes_index.json exists and strictly excludes the note body (ADR Q17)
        val catalogRaw = driver.readText(JsonIndexNoteRepository.CATALOG_FILE)
        assertNotNull(catalogRaw)
        assertTrue(catalogRaw.contains("Secret Vault Architecture"))
        assertFalse(
            catalogRaw.contains("Highly confidential note body text"),
            "Catalog must never contain note body contents (Zero-leakage search)"
        )
    }

    @Test
    fun testLoadNoteReadsDecoupledFile() {
        val note = Note(
            id = "note_202",
            title = "Canvas Meeting",
            content = "Wireframe meeting notes",
            type = NoteType.CANVAS,
            tags = listOf("design"),
            createdAt = 1717031000000L
        )

        repository.saveNote(note)
        val loaded = repository.loadNote("note_202")
        assertNotNull(loaded)
        assertEquals("note_202", loaded.id)
        assertEquals("Canvas Meeting", loaded.title)
        assertEquals("Wireframe meeting notes", loaded.content)
        assertEquals(NoteType.CANVAS, loaded.type)

        val missing = repository.loadNote("non_existent")
        assertNull(missing)
    }

    @Test
    fun testSearchNotesFastInMemory() {
        repository.saveNote(
            Note(id = "1", title = "Kotlin Multiplatform Setup", content = "Body 1", tags = listOf("kmp", "compose"))
        )
        repository.saveNote(
            Note(id = "2", title = "Obsidian Editor Core", content = "Body 2", tags = listOf("editor", "markdown"))
        )
        repository.saveNote(
            Note(id = "3", title = "Samsung Notes Inking", content = "Body 3", tags = listOf("canvas", "skia"))
        )

        // Search by title substring
        val searchKmp = repository.searchNotes("Multiplatform")
        assertEquals(1, searchKmp.size)
        assertEquals("1", searchKmp.first().id)

        // Search by tag
        val searchEditor = repository.searchNotes("markdown")
        assertEquals(1, searchEditor.size)
        assertEquals("2", searchEditor.first().id)

        // Search non-existent
        val searchNone = repository.searchNotes("DoesNotExist")
        assertTrue(searchNone.isEmpty())

        // Empty search returns all
        val searchAll = repository.searchNotes("")
        assertEquals(3, searchAll.size)
    }

    @Test
    fun testFilterByTag() {
        repository.saveNote(
            Note(id = "1", title = "Task 1", tags = listOf("urgent", "work"))
        )
        repository.saveNote(
            Note(id = "2", title = "Task 2", tags = listOf("personal"))
        )

        val urgent = repository.filterByTag("urgent")
        assertEquals(1, urgent.size)
        assertEquals("1", urgent.first().id)

        // Tag with hashtag prefix
        val personal = repository.filterByTag("#personal")
        assertEquals(1, personal.size)
        assertEquals("2", personal.first().id)
    }

    @Test
    fun testDeleteNoteRemovesFileAndCatalogMetadata() {
        val note = Note(id = "del_1", title = "To be deleted", content = "Bye bye")
        repository.saveNote(note)

        val noteFilePath = JsonIndexNoteRepository.noteFilePath("del_1")
        assertTrue(driver.exists(noteFilePath))
        assertEquals(1, repository.getCatalog().notes.size)

        val deleted = repository.deleteNote("del_1")
        assertTrue(deleted)
        assertFalse(driver.exists(noteFilePath), "Note file must be purged")
        assertEquals(0, repository.getCatalog().notes.size)
    }

    @Test
    fun testRebuildCatalogFromOrphanFiles() {
        // Populate driver directly with files, simulating existing vault files with no index
        val note1 = Note(id = "orphan_1", title = "Restored Note 1", tags = listOf("backup"), createdAt = 1000L)
        val note2 = Note(id = "orphan_2", title = "Restored Note 2", tags = listOf("sync"), createdAt = 2000L)

        val repoDirect = JsonIndexNoteRepository(driver)
        repoDirect.saveNote(note1)
        repoDirect.saveNote(note2)

        // Delete catalog file
        driver.delete(JsonIndexNoteRepository.CATALOG_FILE)

        // New repository instance initializes with empty catalog
        val freshRepo = JsonIndexNoteRepository(driver)
        assertEquals(0, freshRepo.getCatalog().notes.size)

        // Rebuild catalog
        val rebuilt = freshRepo.rebuildCatalog()
        assertEquals(2, rebuilt.notes.size)
        assertTrue(driver.exists(JsonIndexNoteRepository.CATALOG_FILE))
        assertTrue(rebuilt.notes.any { it.title == "Restored Note 1" })
        assertTrue(rebuilt.notes.any { it.title == "Restored Note 2" })
    }

    @Test
    fun testVaultStats() {
        repository.saveNote(
            Note(id = "1", title = "Text 1", type = NoteType.TEXT, isEncrypted = false)
        )
        repository.saveNote(
            Note(id = "2", title = "Text 2", type = NoteType.TEXT, isEncrypted = true)
        )
        repository.saveNote(
            Note(id = "3", title = "Canvas 1", type = NoteType.CANVAS, isEncrypted = false)
        )

        val stats = repository.getVaultStats()
        assertEquals(3, stats.totalNotes)
        assertEquals(2, stats.textNotes)
        assertEquals(1, stats.canvasNotes)
        assertEquals(1, stats.encryptedNotes)
        assertTrue(stats.totalSizeBytes > 0)
        assertEquals(1, stats.catalogVersion)
    }
}
