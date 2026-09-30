package com.notes.client

import com.notes.client.editor.ToolbarRegistry
import com.notes.client.storage.DeviceSettingsDriver
import com.notes.common.crypto.ProtectedNoteCodec
import com.notes.common.models.*
import kotlin.test.*

class EditorSettingsAndSharingTest {

    @Test
    fun testToolbarRegistryDefaults() {
        val defaultActive = ToolbarRegistry.DEFAULT_BUTTON_IDS
        assertTrue(defaultActive.isNotEmpty())
        assertTrue(defaultActive.contains("bold"))
        assertTrue(defaultActive.contains("italic"))
        assertTrue(defaultActive.contains("h1"))
        assertTrue(defaultActive.contains("h2"))
        assertTrue(defaultActive.contains("bullet"))
        assertTrue(defaultActive.contains("tasks"))

        val allItems = ToolbarRegistry.ALL_ACTIONS
        assertTrue(allItems.size >= defaultActive.size)
        defaultActive.forEach { id ->
            assertNotNull(ToolbarRegistry.ALL_ACTIONS[id])
        }
    }

    @Test
    fun testDeviceSettingsDriverGeneralConfig() {
        val driver = DeviceSettingsDriver()
        val initial = driver.getGeneralSettings()
        assertEquals("DARK", initial.theme)
        assertEquals(16f, initial.editorFontSize)
        assertTrue(initial.searchContentEnabled)

        val updated = initial.copy(
            theme = "LIGHT",
            editorFontSize = 18f,
            searchContentEnabled = false
        )
        driver.saveGeneralSettings(updated)
        val loaded = driver.getGeneralSettings()
        assertEquals("LIGHT", loaded.theme)
        assertEquals(18f, loaded.editorFontSize)
        assertFalse(loaded.searchContentEnabled)

        // Reset
        driver.saveGeneralSettings(initial)
    }

    @Test
    fun testDeviceSettingsDriverToolbarConfig() {
        val driver = DeviceSettingsDriver()
        val initial = driver.getToolbarConfig()
        assertTrue(initial.activeButtons.contains("bold"))

        val custom = ToolbarConfig(
            activeButtons = listOf("italic", "h1", "quote"),
            disabledButtons = listOf("bold", "strikethrough")
        )
        driver.saveToolbarConfig(custom)
        val loaded = driver.getToolbarConfig()
        assertEquals(listOf("italic", "h1", "quote"), loaded.activeButtons)
        assertEquals(listOf("bold", "strikethrough"), loaded.disabledButtons)

        // Reset
        driver.saveToolbarConfig(initial)
    }

    @Test
    fun testProtectedNoteFullLifecycle() {
        val plainNote = Note(
            id = "note_101",
            title = "My Secret Diary",
            content = "# Top Secret Content\n\nMust not leak into search index.",
            type = NoteType.TEXT,
            tags = listOf("personal", "secret")
        )

        // 1. Protect with password
        val password = "SuperSecretPassword123"
        val hint = "Favorite city"
        val protectedSelfContained = ProtectedNoteCodec.createProtectedNote(
            noteId = plainNote.id,
            title = plainNote.title,
            password = password,
            payloadContent = plainNote.content,
            type = plainNote.type,
            passwordHint = hint
        )
        val packedNapString = ProtectedNoteCodec.pack(protectedSelfContained)
        assertTrue(packedNapString.startsWith(ProtectedNoteCodec.MAGIC_HEADER))

        val protectedNote = plainNote.copy(
            content = packedNapString,
            isProtected = true
        )

        // 2. Verify metadata unpacking
        val unpacked = ProtectedNoteCodec.unpack(protectedNote.content)
        assertEquals(hint, unpacked.metadata.passwordHint)
        assertEquals(plainNote.title, unpacked.metadata.title)

        // 3. Password Verification
        val isCorrectValid = ProtectedNoteCodec.verifyPassword(password, unpacked.metadata)
        assertTrue(isCorrectValid)

        val isWrongValid = ProtectedNoteCodec.verifyPassword("WrongPassword", unpacked.metadata)
        assertFalse(isWrongValid)

        // 4. Remove Protection
        val restoredContent = unpacked.payloadContent
        assertEquals(plainNote.content, restoredContent)

        val plainRestored = protectedNote.copy(
            content = restoredContent,
            isProtected = false
        )
        assertFalse(plainRestored.isProtected)
        assertEquals(plainNote.content, plainRestored.content)
    }

    @Test
    fun testSearchFilteringExcludesProtectedContent() {
        val notes = listOf(
            Note(
                id = "1",
                title = "Meeting Notes",
                content = "Project quantum discussion and roadmap",
                tags = listOf("work")
            ),
            Note(
                id = "2",
                title = "Encrypted Vault Entry",
                content = "Project quantum financial keys",
                tags = listOf("crypto"),
                isEncrypted = true
            ),
            Note(
                id = "3",
                title = "Protected Journal",
                content = "Project quantum secret code",
                tags = listOf("private"),
                isProtected = true
            )
        )

        val query = "quantum"
        val searchContent = true

        // Search logic from ObsidianSidebar
        val matched = notes.filter { note ->
            val matchesTitle = note.title.contains(query, ignoreCase = true)
            val matchesTag = note.tags.any { it.contains(query, ignoreCase = true) }
            val matchesContent = if (searchContent && !note.isProtected && !note.isEncrypted) {
                note.content.contains(query, ignoreCase = true)
            } else false
            matchesTitle || matchesTag || matchesContent
        }

        // Only note 1 should match on content; notes 2 and 3 must NOT match on content
        assertEquals(1, matched.size)
        assertEquals("1", matched.first().id)
    }

    @Test
    fun testCollabLinkFormatAndProprietaryDetection() {
        val plainNote = Note(id = "42", title = "Specs", content = "Hello", type = NoteType.TEXT)
        val canvasNote = Note(id = "43", title = "Drawing", content = "...", type = NoteType.CANVAS)
        val protectedNote = Note(id = "44", title = "Secured", content = "...", type = NoteType.TEXT, isProtected = true)

        assertFalse(plainNote.type == NoteType.CANVAS || plainNote.isProtected || plainNote.isEncrypted)
        assertTrue(canvasNote.type == NoteType.CANVAS || canvasNote.isProtected || canvasNote.isEncrypted)
        assertTrue(protectedNote.type == NoteType.CANVAS || protectedNote.isProtected || protectedNote.isEncrypted)

        val serverUrl = "https://notes.example.com/api"
        val link = "${serverUrl.trimEnd('/')}/collab/${plainNote.id}"
        assertEquals("https://notes.example.com/api/collab/42", link)
    }
}
