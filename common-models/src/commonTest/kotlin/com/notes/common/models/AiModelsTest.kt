package com.notes.common.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiModelsTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    @Test
    fun testAiProviderConfigSerialization() {
        val geminiConfig = AiProviderConfig(
            providerType = AiProviderType.GEMINI,
            apiKey = "test-gemini-key",
            primaryModelId = "gemini-3.5-flash",
            fallbackModelId = "gemini-3.8-flash",
            isFallbackEnabled = true,
            baseUrl = "https://generativelanguage.googleapis.com"
        )

        val serialized = json.encodeToString(geminiConfig)
        val deserialized = json.decodeFromString<AiProviderConfig>(serialized)

        assertEquals(geminiConfig, deserialized)
        assertEquals("gemini-3.5-flash", deserialized.primaryModelId)
        assertEquals("gemini-3.8-flash", deserialized.fallbackModelId)
        assertTrue(deserialized.isFallbackEnabled)
    }

    @Test
    fun testAiSettingsConfigDefaultsAndUpdate() {
        val settings = AiSettingsConfig()

        assertEquals(AiProviderType.GEMINI, settings.activeProvider)
        assertEquals(4, settings.providers.size)

        val active = settings.getActiveConfig()
        assertEquals(AiProviderType.GEMINI, active.providerType)
        assertEquals("gemini-3.5-flash", active.primaryModelId)
        assertEquals("gemini-3.8-flash", active.fallbackModelId)

        // Test Local Server Config Defaults
        val localConfig = settings.providers[AiProviderType.LOCAL_SERVER]
        assertNotNull(localConfig)
        assertEquals("http://localhost:11434", localConfig.baseUrl)
        assertEquals("llama3.3", localConfig.primaryModelId)
        assertEquals(LocalAiProtocol.OPENAI_COMPATIBLE, localConfig.localProtocol)

        // Test updating provider
        val updatedLocal = localConfig.copy(primaryModelId = "mistral-small", baseUrl = "http://192.168.1.100:11434")
        val updatedSettings = settings.updateProvider(updatedLocal)
        assertEquals("mistral-small", updatedSettings.providers[AiProviderType.LOCAL_SERVER]?.primaryModelId)
        assertEquals("http://192.168.1.100:11434", updatedSettings.providers[AiProviderType.LOCAL_SERVER]?.baseUrl)

        // Test serialization
        val serialized = json.encodeToString(updatedSettings)
        val deserialized = json.decodeFromString<AiSettingsConfig>(serialized)
        assertEquals(updatedSettings, deserialized)
    }

    @Test
    fun testAiMetadataRequestAndFillSerialization() {
        val request = AiMetadataRequest(
            noteId = "note-999",
            title = "Untitled Note",
            content = "# KMP and Compose\nMultiplatform reactive apps with AI metadata generation.",
            existingTags = listOf("mobile", "kmp"),
            maxTags = 5
        )

        val reqSerialized = json.encodeToString(request)
        val reqDeserialized = json.decodeFromString<AiMetadataRequest>(reqSerialized)
        assertEquals(request, reqDeserialized)

        val fill = NoteMetadataFill(
            suggestedTitle = "KMP & Compose Architecture",
            suggestedTags = listOf("compose", "reactive", "ai-metadata"),
            summary = "An overview of multiplatform reactive apps with AI metadata generation.",
            suggestedWikilinks = listOf("Architecture Blueprint", "Design System"),
            detectedLanguage = "en"
        )

        val fillSerialized = json.encodeToString(fill)
        val fillDeserialized = json.decodeFromString<NoteMetadataFill>(fillSerialized)
        assertEquals(fill, fillDeserialized)
        assertEquals(3, fillDeserialized.suggestedTags.size)
        assertEquals("KMP & Compose Architecture", fillDeserialized.suggestedTitle)
    }

    @Test
    fun testConnectionTestResultSerialization() {
        val successResult = ConnectionTestResult(
            isSuccess = true,
            latencyMs = 128L,
            modelName = "gemini-3.5-flash",
            errorMessage = null
        )

        val serialized = json.encodeToString(successResult)
        val deserialized = json.decodeFromString<ConnectionTestResult>(serialized)
        assertEquals(successResult, deserialized)
        assertTrue(deserialized.isSuccess)
        assertEquals(128L, deserialized.latencyMs)

        val failResult = ConnectionTestResult(
            isSuccess = false,
            latencyMs = 0L,
            errorMessage = "HTTP 401 Unauthorized"
        )
        val failSerialized = json.encodeToString(failResult)
        val failDeserialized = json.decodeFromString<ConnectionTestResult>(failSerialized)
        assertEquals(failResult, failDeserialized)
    }
}
