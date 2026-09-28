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

    @Test
    fun testAiModelCatalogDefaultLoading() {
        val catalog = AiModelCatalog.defaultCatalog()
        assertEquals("1.0.0", catalog.schemaVersion)
        assertEquals("1.0.0", catalog.catalogVersion)
        assertEquals(4, catalog.providers.size)

        // Gemini
        val geminiModels = catalog.getModelsForProvider(AiProviderType.GEMINI)
        assertTrue(geminiModels.isNotEmpty())
        assertEquals("gemini-3.5-flash", catalog.getPrimaryModel(AiProviderType.GEMINI))
        assertEquals("gemini-3.8-flash", catalog.getFallbackModel(AiProviderType.GEMINI))
        assertTrue(geminiModels.any { it.id == "gemini-3.5-flash" && it.isRecommended })

        // OpenAI
        val openAiModels = catalog.getModelsForProvider(AiProviderType.OPENAI)
        assertEquals("gpt-4o-mini", catalog.getPrimaryModel(AiProviderType.OPENAI))
        assertEquals("gpt-4o", catalog.getFallbackModel(AiProviderType.OPENAI))

        // Anthropic
        assertEquals("claude-3-5-haiku-20241022", catalog.getPrimaryModel(AiProviderType.ANTHROPIC))
        assertEquals("claude-3-7-sonnet", catalog.getFallbackModel(AiProviderType.ANTHROPIC))

        // Local Server
        assertEquals("llama3.3", catalog.getPrimaryModel(AiProviderType.LOCAL_SERVER))
        assertEquals("llama3.2", catalog.getFallbackModel(AiProviderType.LOCAL_SERVER))
    }

    @Test
    fun testAiModelCatalogCustomJsonParsingAndFiltering() {
        val customJson = """{
            "schemaVersion": "1.1.0",
            "catalogVersion": "2.0.0",
            "updatedAt": "2026-10-01T00:00:00Z",
            "providers": {
                "GEMINI": {
                    "provider": "GEMINI",
                    "baseUrl": "https://custom.gemini.proxy",
                    "defaultPrimaryModelId": "gemini-4.0-flash",
                    "defaultFallbackModelId": "gemini-3.8-flash",
                    "models": [
                        {"id": "gemini-4.0-flash", "displayName": "Gemini 4.0 Flash", "tier": "PRIMARY", "isRecommended": true, "status": "ACTIVE"},
                        {"id": "gemini-3.8-flash", "displayName": "Gemini 3.8 Flash", "tier": "FALLBACK", "isRecommended": true, "status": "ACTIVE"},
                        {"id": "gemini-2.5-flash", "displayName": "Gemini 2.5 Flash (Legacy)", "tier": "FAST", "status": "SUNSET"}
                    ]
                }
            }
        }"""

        val catalog = AiModelCatalog.loadFromJson(customJson)
        assertEquals("2.0.0", catalog.catalogVersion)
        assertEquals("gemini-4.0-flash", catalog.getPrimaryModel(AiProviderType.GEMINI))

        val models = catalog.getModelsForProvider(AiProviderType.GEMINI)
        // Ensure SUNSET model gemini-2.5-flash is filtered out
        assertEquals(2, models.size)
        assertTrue(models.none { it.id == "gemini-2.5-flash" })
        assertTrue(models.any { it.id == "gemini-4.0-flash" })
    }

    @Test
    fun testAiModelCatalogCorruptedJsonGracefulFallback() {
        val malformedJson = "{ this is not valid json content [ {"
        val catalog = AiModelCatalog.loadFromJson(malformedJson)

        // Must not throw exception, must return default catalog
        assertNotNull(catalog)
        assertEquals("gemini-3.5-flash", catalog.getPrimaryModel(AiProviderType.GEMINI))
        assertTrue(catalog.getModelsForProvider(AiProviderType.GEMINI).isNotEmpty())

        val emptyCatalog = AiModelCatalog.loadFromJson("")
        assertNotNull(emptyCatalog)
        assertEquals("gemini-3.5-flash", emptyCatalog.getPrimaryModel(AiProviderType.GEMINI))
    }

    @Test
    fun testAiSettingsConfigWithCustomCatalog() {
        val customJson = """{
            "catalogVersion": "3.0.0",
            "providers": {
                "GEMINI": {
                    "provider": "GEMINI",
                    "baseUrl": "https://custom.gemini",
                    "defaultPrimaryModelId": "gemini-future-flash",
                    "defaultFallbackModelId": "gemini-future-pro"
                }
            }
        }"""
        val customCatalog = AiModelCatalog.loadFromJson(customJson)
        val defaultProviders = AiSettingsConfig.defaultProviders(customCatalog)

        val gemini = defaultProviders[AiProviderType.GEMINI]
        assertNotNull(gemini)
        assertEquals("gemini-future-flash", gemini.primaryModelId)
        assertEquals("gemini-future-pro", gemini.fallbackModelId)
        assertEquals("https://custom.gemini", gemini.baseUrl)
    }
}

