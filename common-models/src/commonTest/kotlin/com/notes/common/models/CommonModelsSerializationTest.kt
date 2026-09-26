package com.notes.common.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonModelsSerializationTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    @Test
    fun testNoteSerialization() {
        val note = Note(
            id = "note-123",
            title = "Test Note Title",
            content = "# Header\nContent markdown",
            type = NoteType.TEXT,
            tags = listOf("work/project1", "meeting"),
            isEncrypted = false,
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L,
            version = 4L
        )

        val serialized = json.encodeToString(note)
        val deserialized = json.decodeFromString<Note>(serialized)

        assertEquals(note, deserialized)
        assertEquals("Test Note Title", deserialized.title)
        assertEquals(NoteType.TEXT, deserialized.type)
        assertEquals(2, deserialized.tags.size)
    }

    @Test
    fun testCanvasModelsSerialization() {
        val p1 = InkPoint(x = 10.5f, y = 20.0f, pressure = 0.8f, tilt = 0.1f, timestamp = 100L)
        val p2 = InkPoint(x = 15.0f, y = 25.5f, pressure = 0.9f, tilt = 0.2f, timestamp = 120L)

        val stroke = InkStroke(
            id = "stroke-1",
            tool = ToolType.PEN,
            colorHex = "#FF0000",
            strokeWidth = 3.5f,
            opacity = 0.95f,
            points = listOf(p1, p2)
        )

        val layer = CanvasLayer(
            id = "layer-1",
            name = "Drawing Layer",
            zIndex = 1,
            isVisible = true,
            opacity = 1.0f,
            layerType = LayerType.VECTOR,
            strokes = listOf(stroke)
        )

        val manifest = CmnManifest(
            version = 1,
            noteId = "canvas-note-999",
            title = "Canvas Architecture Diagram",
            layers = listOf(layer),
            createdAt = 1700000000000L,
            updatedAt = 1700000010000L
        )

        val serialized = json.encodeToString(manifest)
        val deserialized = json.decodeFromString<CmnManifest>(serialized)

        assertEquals(manifest, deserialized)
        assertEquals(1, deserialized.layers.size)
        assertEquals(1, deserialized.layers[0].strokes.size)
        assertEquals(2, deserialized.layers[0].strokes[0].points.size)
        assertEquals(ToolType.PEN, deserialized.layers[0].strokes[0].tool)
    }

    @Test
    fun testStorageCatalogSerialization() {
        val meta1 = NoteMetadata(
            id = "note-1",
            title = "Personal Diary",
            tags = listOf("journal"),
            type = NoteType.TEXT,
            isEncrypted = true,
            updatedAt = 1700000000000L,
            sizeBytes = 1024L
        )
        val meta2 = NoteMetadata(
            id = "note-2",
            title = "Brainstorming Sketch",
            tags = listOf("sketch", "design"),
            type = NoteType.CANVAS,
            isEncrypted = false,
            updatedAt = 1700000020000L,
            sizeBytes = 20480L
        )

        val catalog = NotesIndexCatalog(
            version = 1,
            lastSyncedAt = 1700000030000L,
            notes = listOf(meta1, meta2)
        )

        val serialized = json.encodeToString(catalog)
        val deserialized = json.decodeFromString<NotesIndexCatalog>(serialized)

        assertEquals(catalog, deserialized)
        assertEquals(2, deserialized.notes.size)
        assertTrue(deserialized.notes[0].isEncrypted)
        assertEquals(NoteType.CANVAS, deserialized.notes[1].type)
    }

    @Test
    fun testSyncDTOsSerialization() {
        val note = Note(id = "n-1", title = "Synced Note", content = "Synced body")
        val req = SyncRequest(
            clientId = "device-abc-123",
            lastSyncedTimestamp = 1699999900000L,
            modifiedNotes = listOf(note),
            deletedNoteIds = listOf("deleted-id-1")
        )

        val serializedReq = json.encodeToString(req)
        val deserializedReq = json.decodeFromString<SyncRequest>(serializedReq)

        assertEquals(req, deserializedReq)
        assertEquals(1, deserializedReq.modifiedNotes.size)
        assertEquals("deleted-id-1", deserializedReq.deletedNoteIds[0])

        val resp = SyncResponse(
            serverTimestamp = 1700000000000L,
            serverUpdates = listOf(note),
            serverDeletions = listOf("old-id"),
            conflictsResolved = 1
        )
        val serializedResp = json.encodeToString(resp)
        val deserializedResp = json.decodeFromString<SyncResponse>(serializedResp)

        assertEquals(resp, deserializedResp)
        assertEquals(1, deserializedResp.conflictsResolved)

        val authReq = AuthRequest(email = "user@example.com", passwordHash = "argon2id_hash_sample")
        val authResp = AuthResponse(
            accessToken = "access_jwt_token",
            refreshToken = "refresh_jwt_token",
            userId = "usr-42",
            email = "user@example.com",
            expiresIn = 3600L
        )

        val deserializedAuthReq = json.decodeFromString<AuthRequest>(json.encodeToString(authReq))
        val deserializedAuthResp = json.decodeFromString<AuthResponse>(json.encodeToString(authResp))

        assertEquals(authReq, deserializedAuthReq)
        assertEquals(authResp, deserializedAuthResp)
    }
}
