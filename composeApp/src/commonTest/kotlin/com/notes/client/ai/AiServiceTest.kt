package com.notes.client.ai

import com.notes.common.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MockHttpTransport(
    var handler: (url: String, method: String, headers: Map<String, String>, body: String?) -> HttpResponseData
) : HttpTransport {
    val recordedCalls = mutableListOf<String>()

    override suspend fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): HttpResponseData {
        recordedCalls.add("$method $url")
        return handler(url, method, headers, body)
    }
}

class AiServiceTest {

    @Test
    fun testContextTruncatorWithinLimits() {
        val title = "Sample Title"
        val content = "Short content paragraph."
        val result = ContextTruncator.truncate(title, content, maxChars = 1000)

        assertTrue(result.contains("Title: Sample Title"))
        assertTrue(result.contains("Short content paragraph."))
        assertFalse(result.contains("truncated"))
    }

    @Test
    fun testContextTruncatorExceedingLimitsPreservesHeadings() {
        val title = "Large Spec Document"
        val builder = StringBuilder()
        builder.append("# Section 1: Intro\nSome text\n")
        builder.append("## Section 2: Details\nSome more text\n")
        for (i in 1..500) {
            builder.append("Paragraph line $i with details and notes.\n")
        }
        val fullContent = builder.toString()

        val result = ContextTruncator.truncate(title, fullContent, maxChars = 500)
        assertTrue(result.length <= 600)
        assertTrue(result.contains("Key Headings Outline:"))
        assertTrue(result.contains("# Section 1: Intro"))
        assertTrue(result.contains("[Content truncated for AI context limits]"))
    }

    @Test
    fun testPromptBuilderContainsExistingTagsClause() {
        val prompt = PromptBuilder.buildSystemInstruction(maxTags = 6, existingTags = listOf("architecture", "kmp"))
        assertTrue(prompt.contains("architecture"))
        assertTrue(prompt.contains("kmp"))
        assertTrue(prompt.contains("Do NOT repeat these existing tags"))
        assertTrue(prompt.contains("6"))
    }

    @Test
    fun testJsonSanitizerWithCodeFencedJson() {
        val rawLlm = """
            Here are the suggestions for your note:
            ```json
            {
              "suggestedTitle": "Jetpack Compose Guide",
              "suggestedTags": ["#COMPOSE", "UI-UX", "android"],
              "summary": "Comprehensive guide for modern Android UI.",
              "suggestedWikilinks": ["[[Architecture]]", "Navigation"]
            }
            ```
            Hope this helps!
        """.trimIndent()

        val result = JsonSanitizer.parseMetadataResponse(rawLlm, existingTags = listOf("android"))

        assertEquals("Jetpack Compose Guide", result.suggestedTitle)
        assertEquals("Comprehensive guide for modern Android UI.", result.summary)
        // #COMPOSE normalized to compose, UI-UX normalized to ui-ux, android filtered out
        assertEquals(listOf("compose", "ui-ux"), result.suggestedTags)
        assertEquals(listOf("Architecture", "Navigation"), result.suggestedWikilinks)
    }

    @Test
    fun testJsonSanitizerFallbackRegexExtraction() {
        val rawLlm = """
            I could not format as JSON, but here are tags:
            #kotlin-multiplatform #Compose #AI
            Summary: Overview of multiplatform features.
        """.trimIndent()

        val result = JsonSanitizer.parseMetadataResponse(rawLlm, existingTags = emptyList())
        assertEquals(listOf("kotlin-multiplatform", "compose", "ai"), result.suggestedTags)
    }

    @Test
    fun testGeminiHappyPathExecution() = runTest {
        val mockTransport = MockHttpTransport { url, _, _, _ ->
            assertTrue(url.contains("gemini-3.5-flash"))
            HttpResponseData(
                statusCode = 200,
                body = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": "{\"suggestedTitle\": \"KMP Architecture\", \"suggestedTags\": [\"kmp\", \"architecture\"], \"summary\": \"Architecture overview.\"}"
                              }
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        val service = DefaultAiClientService(mockTransport)
        val config = AiProviderConfig(
            providerType = AiProviderType.GEMINI,
            apiKey = "test-key",
            primaryModelId = "gemini-3.5-flash",
            fallbackModelId = "gemini-3.8-flash"
        )
        val request = AiMetadataRequest(noteId = "1", title = "Untitled", content = "Kotlin Multiplatform code.")

        val result = service.fillMetadata(request, config).getOrThrow()
        assertEquals("KMP Architecture", result.suggestedTitle)
        assertEquals(listOf("kmp", "architecture"), result.suggestedTags)
        assertEquals(1, mockTransport.recordedCalls.size)
    }

    @Test
    fun testGeminiAutoFailoverTo38FlashOn404() = runTest {
        val mockTransport = MockHttpTransport { url, _, _, _ ->
            if (url.contains("gemini-3.5-flash")) {
                // Primary is deprecated or 404
                HttpResponseData(statusCode = 404, body = "{\"error\": \"Model gemini-3.5-flash not found or deactivated\"}")
            } else {
                // Fallback to gemini-3.8-flash succeeds
                assertTrue(url.contains("gemini-3.8-flash"))
                HttpResponseData(
                    statusCode = 200,
                    body = """
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "{\"suggestedTitle\": \"Failover Resilient Title\", \"suggestedTags\": [\"gemini\", \"fallback\"], \"summary\": \"Handled via Gemini 3.8 Flash failover.\"}"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                    """.trimIndent()
                )
            }
        }

        val service = DefaultAiClientService(mockTransport)
        val config = AiProviderConfig(
            providerType = AiProviderType.GEMINI,
            apiKey = "test-key",
            primaryModelId = "gemini-3.5-flash",
            fallbackModelId = "gemini-3.8-flash",
            isFallbackEnabled = true
        )
        val request = AiMetadataRequest(noteId = "1", title = "Untitled", content = "Testing failover mechanism.")

        val result = service.fillMetadata(request, config).getOrThrow()
        assertEquals("Failover Resilient Title", result.suggestedTitle)
        assertEquals(listOf("gemini", "fallback"), result.suggestedTags)
        assertEquals(2, mockTransport.recordedCalls.size)
        assertTrue(mockTransport.recordedCalls[0].contains("gemini-3.5-flash"))
        assertTrue(mockTransport.recordedCalls[1].contains("gemini-3.8-flash"))
    }

    @Test
    fun testOpenAiExecution() = runTest {
        val mockTransport = MockHttpTransport { _, _, headers, _ ->
            assertEquals("Bearer test-openai-key", headers["Authorization"])
            HttpResponseData(
                statusCode = 200,
                body = """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"suggestedTitle\": \"OpenAI Note\", \"suggestedTags\": [\"gpt\", \"ai\"]}"
                          }
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        val service = DefaultAiClientService(mockTransport)
        val config = AiProviderConfig(
            providerType = AiProviderType.OPENAI,
            apiKey = "test-openai-key",
            primaryModelId = "gpt-4o-mini"
        )
        val request = AiMetadataRequest(noteId = "2", title = "Note", content = "Content")
        val result = service.fillMetadata(request, config).getOrThrow()
        assertEquals("OpenAI Note", result.suggestedTitle)
        assertEquals(listOf("gpt", "ai"), result.suggestedTags)
    }

    @Test
    fun testAnthropicExecution() = runTest {
        val mockTransport = MockHttpTransport { _, _, headers, _ ->
            assertEquals("test-anthropic-key", headers["x-api-key"])
            HttpResponseData(
                statusCode = 200,
                body = """
                    {
                      "content": [
                        {
                          "type": "text",
                          "text": "{\"suggestedTitle\": \"Claude Note\", \"suggestedTags\": [\"claude\", \"anthropic\"]}"
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        val service = DefaultAiClientService(mockTransport)
        val config = AiProviderConfig(
            providerType = AiProviderType.ANTHROPIC,
            apiKey = "test-anthropic-key",
            primaryModelId = "claude-3-5-haiku-20241022"
        )
        val request = AiMetadataRequest(noteId = "3", title = "Note", content = "Content")
        val result = service.fillMetadata(request, config).getOrThrow()
        assertEquals("Claude Note", result.suggestedTitle)
        assertEquals(listOf("claude", "anthropic"), result.suggestedTags)
    }

    @Test
    fun testLocalOllamaExecutionAndDiscovery() = runTest {
        val mockTransport = MockHttpTransport { url, _, _, _ ->
            if (url.endsWith("/api/tags")) {
                HttpResponseData(
                    statusCode = 200,
                    body = """
                        {
                          "models": [
                            { "name": "llama3.3:latest" },
                            { "name": "qwen2.5:7b" },
                            { "name": "mistral:latest" }
                          ]
                        }
                    """.trimIndent()
                )
            } else {
                HttpResponseData(
                    statusCode = 200,
                    body = """
                        {
                          "response": "{\"suggestedTitle\": \"Local Model Note\", \"suggestedTags\": [\"local\", \"ollama\"]}"
                        }
                    """.trimIndent()
                )
            }
        }

        val service = DefaultAiClientService(mockTransport)
        val config = AiProviderConfig(
            providerType = AiProviderType.LOCAL_SERVER,
            baseUrl = "http://localhost:11434",
            primaryModelId = "llama3.3",
            localProtocol = LocalAiProtocol.OLLAMA_NATIVE
        )

        // 1. Test Model Discovery
        val discoveredModels = service.discoverLocalModels("http://localhost:11434", LocalAiProtocol.OLLAMA_NATIVE)
        assertEquals(listOf("llama3.3:latest", "qwen2.5:7b", "mistral:latest"), discoveredModels)

        // 2. Test Metadata Fill
        val request = AiMetadataRequest(noteId = "4", title = "Note", content = "Private local text")
        val result = service.fillMetadata(request, config).getOrThrow()
        assertEquals("Local Model Note", result.suggestedTitle)
        assertEquals(listOf("local", "ollama"), result.suggestedTags)
    }

    @Test
    fun testConnectionTestResults() = runTest {
        // Success test
        val mockSuccess = MockHttpTransport { _, _, _, _ ->
            HttpResponseData(statusCode = 200, body = "{\"models\": []}")
        }
        val service = DefaultAiClientService(mockSuccess)
        val resSuccess = service.testConnection(
            AiProviderConfig(providerType = AiProviderType.GEMINI, apiKey = "valid-key")
        )
        assertTrue(resSuccess.isSuccess)
        assertNull(resSuccess.errorMessage)

        // Fail test (401 Unauthorized)
        val mockFail = MockHttpTransport { _, _, _, _ ->
            HttpResponseData(statusCode = 401, body = "Invalid API Key")
        }
        val failService = DefaultAiClientService(mockFail)
        val resFail = failService.testConnection(
            AiProviderConfig(providerType = AiProviderType.OPENAI, apiKey = "bad-key")
        )
        assertFalse(resFail.isSuccess)
        assertTrue(resFail.errorMessage?.contains("401") == true)
    }
}
