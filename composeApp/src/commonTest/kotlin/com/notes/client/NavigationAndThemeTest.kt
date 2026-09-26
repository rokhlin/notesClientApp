package com.notes.client

import com.notes.client.navigation.CanvasRoute
import com.notes.client.navigation.NoteDetailRoute
import com.notes.client.navigation.NoteListRoute
import com.notes.client.navigation.SettingsRoute
import com.notes.client.theme.BrandAmber
import com.notes.client.theme.BrandCyan
import com.notes.client.theme.BrandIndigo
import com.notes.client.theme.DarkColorScheme
import com.notes.client.theme.LightColorScheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NavigationAndThemeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDestinationSerialization() {
        val listRoute = NoteListRoute
        assertEquals("All Notes", listRoute.title)

        val detailRoute = NoteDetailRoute(noteId = "note-42")
        assertEquals("Text Editor", detailRoute.title)
        val encodedDetail = json.encodeToString(detailRoute)
        val decodedDetail = json.decodeFromString<NoteDetailRoute>(encodedDetail)
        assertEquals("note-42", decodedDetail.noteId)

        val canvasRoute = CanvasRoute(noteId = "canvas-99")
        assertEquals("Canvas", canvasRoute.title)
        val decodedCanvas = json.decodeFromString<CanvasRoute>(json.encodeToString(canvasRoute))
        assertEquals("canvas-99", decodedCanvas.noteId)

        val settingsRoute = SettingsRoute
        assertEquals("Settings", settingsRoute.title)
    }

    @Test
    fun testThemeColorTokens() {
        assertEquals(BrandIndigo, LightColorScheme.primary)
        assertEquals(BrandIndigo, DarkColorScheme.primary)

        assertEquals(BrandCyan, LightColorScheme.secondary)
        assertEquals(BrandCyan, DarkColorScheme.secondary)

        assertEquals(BrandAmber, LightColorScheme.tertiary)
        assertEquals(BrandAmber, DarkColorScheme.tertiary)

        // Verify high-contrast distinction between light and dark surfaces
        assertNotEquals(LightColorScheme.background, DarkColorScheme.background)
        assertNotEquals(LightColorScheme.surface, DarkColorScheme.surface)
        assertNotEquals(LightColorScheme.surfaceVariant, DarkColorScheme.surfaceVariant)
    }
}
