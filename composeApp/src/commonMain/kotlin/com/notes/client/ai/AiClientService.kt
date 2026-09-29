package com.notes.client.ai

import com.notes.common.models.*
import com.notes.client.util.currentTimeMillis
import kotlinx.serialization.json.*

interface AiClientService {
    suspend fun testConnection(config: AiProviderConfig): ConnectionTestResult
    suspend fun fillMetadata(request: AiMetadataRequest, config: AiProviderConfig): Result<NoteMetadataFill>
    suspend fun discoverLocalModels(serverUrl: String, protocol: LocalAiProtocol): List<String>
}

class DefaultAiClientService(
    private val httpTransport: HttpTransport = DefaultHttpTransport()
) : AiClientService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun testConnection(config: AiProviderConfig): ConnectionTestResult {
        val startTime = currentTimeMillis()
        return try {
            when (config.providerType) {
                AiProviderType.GEMINI -> testGeminiConnection(config, startTime)
                AiProviderType.OPENAI -> testOpenAiConnection(config, startTime)
                AiProviderType.ANTHROPIC -> testAnthropicConnection(config, startTime)
                AiProviderType.LOCAL_SERVER -> testLocalServerConnection(config, startTime)
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                latencyMs = currentTimeMillis() - startTime,
                errorMessage = e.message ?: "Connection failed"
            )
        }
    }

    override suspend fun fillMetadata(
        request: AiMetadataRequest,
        config: AiProviderConfig
    ): Result<NoteMetadataFill> {
        return try {
            val systemInstruction = PromptBuilder.buildSystemInstruction(request.maxTags, request.existingTags)
            val userContent = PromptBuilder.buildUserContent(request)

            val rawText = when (config.providerType) {
                AiProviderType.GEMINI -> executeGemini(config, systemInstruction, userContent)
                AiProviderType.OPENAI -> executeOpenAi(config, systemInstruction, userContent)
                AiProviderType.ANTHROPIC -> executeAnthropic(config, systemInstruction, userContent)
                AiProviderType.LOCAL_SERVER -> executeLocalServer(config, systemInstruction, userContent)
            }

            val parsed = JsonSanitizer.parseMetadataResponse(rawText, request.existingTags)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun discoverLocalModels(serverUrl: String, protocol: LocalAiProtocol): List<String> {
        val baseUrl = serverUrl.trim().removeSuffix("/")
        return try {
            if (protocol == LocalAiProtocol.OLLAMA_NATIVE || baseUrl.contains("11434")) {
                val resp = httpTransport.execute("$baseUrl/api/tags", method = "GET")
                if (resp.statusCode in 200..299) {
                    val root = json.parseToJsonElement(resp.body).jsonObject
                    root["models"]?.jsonArray?.mapNotNull {
                        it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    } ?: emptyList()
                } else emptyList()
            } else {
                val resp = httpTransport.execute("$baseUrl/v1/models", method = "GET")
                if (resp.statusCode in 200..299) {
                    val root = json.parseToJsonElement(resp.body).jsonObject
                    root["data"]?.jsonArray?.mapNotNull {
                        it.jsonObject["id"]?.jsonPrimitive?.contentOrNull
                    } ?: emptyList()
                } else emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // Gemini Execution & Auto-Failover
    // -------------------------------------------------------------

    private suspend fun executeGemini(
        config: AiProviderConfig,
        systemInstruction: String,
        userContent: String
    ): String {
        val primaryModel = config.primaryModelId.ifBlank { "gemini-3.5-flash" }
        val primaryRes = dispatchGeminiRequest(config, primaryModel, systemInstruction, userContent)

        // Check if primary succeeded
        if (primaryRes.statusCode in 200..299) {
            return extractGeminiText(primaryRes.body)
        }

        // Auto-failover condition (404/410 model deprecated/not found or 429 quota exhausted)
        val fallback = config.fallbackModelId
        if (config.isFallbackEnabled && !fallback.isNullOrBlank() &&
            (primaryRes.statusCode == 404 || primaryRes.statusCode == 410 || primaryRes.statusCode == 429)
        ) {
            val fallbackRes = dispatchGeminiRequest(config, fallback, systemInstruction, userContent)
            if (fallbackRes.statusCode in 200..299) {
                return extractGeminiText(fallbackRes.body)
            }
            throw IllegalStateException("Gemini fallback model '$fallback' failed with HTTP ${fallbackRes.statusCode}: ${fallbackRes.body.take(200)}")
        }

        throw IllegalStateException("Gemini primary model '$primaryModel' failed with HTTP ${primaryRes.statusCode}: ${primaryRes.body.take(200)}")
    }

    private suspend fun dispatchGeminiRequest(
        config: AiProviderConfig,
        modelId: String,
        systemInstruction: String,
        userContent: String
    ): HttpResponseData {
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val url = "$cleanBaseUrl/v1beta/models/$modelId:generateContent?key=${config.apiKey.trim()}"

        val requestJson = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", "$systemInstruction\n\n--- Note Content ---\n$userContent")
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }.toString()

        return httpTransport.execute(
            url = url,
            method = "POST",
            headers = mapOf("Content-Type" to "application/json"),
            body = requestJson
        )
    }

    private fun extractGeminiText(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val candidates = root["candidates"]?.jsonArray
        val firstCandidate = candidates?.firstOrNull()?.jsonObject
        val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
        return parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: responseBody
    }

    // -------------------------------------------------------------
    // OpenAI Execution
    // -------------------------------------------------------------

    private suspend fun executeOpenAi(
        config: AiProviderConfig,
        systemInstruction: String,
        userContent: String
    ): String {
        val primaryModel = config.primaryModelId.ifBlank { "gpt-4o-mini" }
        val primaryRes = dispatchOpenAiRequest(config, primaryModel, systemInstruction, userContent)

        if (primaryRes.statusCode in 200..299) {
            return extractOpenAiText(primaryRes.body)
        }

        val fallback = config.fallbackModelId
        if (config.isFallbackEnabled && !fallback.isNullOrBlank() &&
            (primaryRes.statusCode == 404 || primaryRes.statusCode == 410 || primaryRes.statusCode == 429)
        ) {
            val fallbackRes = dispatchOpenAiRequest(config, fallback, systemInstruction, userContent)
            if (fallbackRes.statusCode in 200..299) {
                return extractOpenAiText(fallbackRes.body)
            }
        }

        throw IllegalStateException("OpenAI failed with HTTP ${primaryRes.statusCode}: ${primaryRes.body.take(200)}")
    }

    private suspend fun dispatchOpenAiRequest(
        config: AiProviderConfig,
        modelId: String,
        systemInstruction: String,
        userContent: String
    ): HttpResponseData {
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val endpoint = if (cleanBaseUrl.endsWith("/chat/completions")) cleanBaseUrl else "$cleanBaseUrl/chat/completions"

        val requestJson = buildJsonObject {
            put("model", modelId)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemInstruction)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", userContent)
                }
            }
            putJsonObject("response_format") {
                put("type", "json_object")
            }
        }.toString()

        val headers = mutableMapOf("Content-Type" to "application/json")
        if (config.apiKey.isNotBlank()) {
            headers["Authorization"] = "Bearer ${config.apiKey.trim()}"
        }

        return httpTransport.execute(url = endpoint, method = "POST", headers = headers, body = requestJson)
    }

    private fun extractOpenAiText(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val choices = root["choices"]?.jsonArray
        val message = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
        return message?.get("content")?.jsonPrimitive?.content ?: responseBody
    }

    // -------------------------------------------------------------
    // Anthropic Execution
    // -------------------------------------------------------------

    private suspend fun executeAnthropic(
        config: AiProviderConfig,
        systemInstruction: String,
        userContent: String
    ): String {
        val primaryModel = config.primaryModelId.ifBlank { "claude-3-5-haiku-20241022" }
        val primaryRes = dispatchAnthropicRequest(config, primaryModel, systemInstruction, userContent)

        if (primaryRes.statusCode in 200..299) {
            return extractAnthropicText(primaryRes.body)
        }

        val fallback = config.fallbackModelId
        if (config.isFallbackEnabled && !fallback.isNullOrBlank() &&
            (primaryRes.statusCode == 404 || primaryRes.statusCode == 410 || primaryRes.statusCode == 429)
        ) {
            val fallbackRes = dispatchAnthropicRequest(config, fallback, systemInstruction, userContent)
            if (fallbackRes.statusCode in 200..299) {
                return extractAnthropicText(fallbackRes.body)
            }
        }

        throw IllegalStateException("Anthropic failed with HTTP ${primaryRes.statusCode}: ${primaryRes.body.take(200)}")
    }

    private suspend fun dispatchAnthropicRequest(
        config: AiProviderConfig,
        modelId: String,
        systemInstruction: String,
        userContent: String
    ): HttpResponseData {
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val endpoint = if (cleanBaseUrl.endsWith("/messages")) cleanBaseUrl else "$cleanBaseUrl/messages"

        val requestJson = buildJsonObject {
            put("model", modelId)
            put("max_tokens", 1024)
            put("system", systemInstruction)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", userContent)
                }
            }
        }.toString()

        val headers = mapOf(
            "Content-Type" to "application/json",
            "x-api-key" to config.apiKey.trim(),
            "anthropic-version" to "2023-06-01"
        )

        return httpTransport.execute(url = endpoint, method = "POST", headers = headers, body = requestJson)
    }

    private fun extractAnthropicText(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val content = root["content"]?.jsonArray
        val textBlock = content?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }?.jsonObject
        return textBlock?.get("text")?.jsonPrimitive?.content ?: responseBody
    }

    // -------------------------------------------------------------
    // Local LLM Server Execution
    // -------------------------------------------------------------

    private suspend fun executeLocalServer(
        config: AiProviderConfig,
        systemInstruction: String,
        userContent: String
    ): String {
        if (config.localProtocol == LocalAiProtocol.OLLAMA_NATIVE) {
            val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
            val endpoint = "$cleanBaseUrl/api/generate"

            val requestJson = buildJsonObject {
                put("model", config.primaryModelId.ifBlank { "llama3.3" })
                put("prompt", "$systemInstruction\n\n--- Note Content ---\n$userContent")
                put("format", "json")
                put("stream", false)
            }.toString()

            val resp = httpTransport.execute(url = endpoint, method = "POST", headers = mapOf("Content-Type" to "application/json"), body = requestJson)
            if (resp.statusCode in 200..299) {
                val root = json.parseToJsonElement(resp.body).jsonObject
                return root["response"]?.jsonPrimitive?.content ?: resp.body
            }
            throw IllegalStateException("Local Ollama server failed with HTTP ${resp.statusCode}: ${resp.body.take(200)}")
        } else {
            return executeOpenAi(config, systemInstruction, userContent)
        }
    }

    // -------------------------------------------------------------
    // Connection Testing Implementations
    // -------------------------------------------------------------

    private suspend fun testGeminiConnection(config: AiProviderConfig, startTime: Long): ConnectionTestResult {
        if (config.apiKey.isBlank()) {
            return ConnectionTestResult(isSuccess = false, errorMessage = "API key cannot be empty")
        }
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val url = "$cleanBaseUrl/v1beta/models?key=${config.apiKey.trim()}"
        val resp = httpTransport.execute(url = url, method = "GET")
        val latency = currentTimeMillis() - startTime

        return if (resp.statusCode in 200..299) {
            ConnectionTestResult(
                isSuccess = true,
                latencyMs = latency,
                modelName = config.primaryModelId
            )
        } else {
            ConnectionTestResult(
                isSuccess = false,
                latencyMs = latency,
                errorMessage = "HTTP ${resp.statusCode} ${resp.body.take(120)}"
            )
        }
    }

    private suspend fun testOpenAiConnection(config: AiProviderConfig, startTime: Long): ConnectionTestResult {
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val url = if (cleanBaseUrl.endsWith("/models")) cleanBaseUrl else "$cleanBaseUrl/models"
        val headers = mutableMapOf<String, String>()
        if (config.apiKey.isNotBlank()) headers["Authorization"] = "Bearer ${config.apiKey.trim()}"

        val resp = httpTransport.execute(url = url, method = "GET", headers = headers)
        val latency = currentTimeMillis() - startTime

        return if (resp.statusCode in 200..299) {
            ConnectionTestResult(isSuccess = true, latencyMs = latency, modelName = config.primaryModelId)
        } else {
            ConnectionTestResult(isSuccess = false, latencyMs = latency, errorMessage = "HTTP ${resp.statusCode} ${resp.body.take(120)}")
        }
    }

    private suspend fun testAnthropicConnection(config: AiProviderConfig, startTime: Long): ConnectionTestResult {
        if (config.apiKey.isBlank()) {
            return ConnectionTestResult(isSuccess = false, errorMessage = "Anthropic API key cannot be empty")
        }
        val startTimeMs = currentTimeMillis()
        val testRes = dispatchAnthropicRequest(config, config.primaryModelId, "Test connection", "ping")
        val latency = currentTimeMillis() - startTimeMs

        return if (testRes.statusCode in 200..299) {
            ConnectionTestResult(isSuccess = true, latencyMs = latency, modelName = config.primaryModelId)
        } else {
            ConnectionTestResult(isSuccess = false, latencyMs = latency, errorMessage = "HTTP ${testRes.statusCode} ${testRes.body.take(120)}")
        }
    }

    private suspend fun testLocalServerConnection(config: AiProviderConfig, startTime: Long): ConnectionTestResult {
        val baseUrl = config.baseUrl.trim().removeSuffix("/")
        val testUrl = if (config.localProtocol == LocalAiProtocol.OLLAMA_NATIVE || baseUrl.contains("11434")) {
            "$baseUrl/api/tags"
        } else {
            "$baseUrl/v1/models"
        }
        val resp = httpTransport.execute(url = testUrl, method = "GET")
        val latency = currentTimeMillis() - startTime

        return if (resp.statusCode in 200..299) {
            ConnectionTestResult(isSuccess = true, latencyMs = latency, modelName = config.primaryModelId)
        } else {
            ConnectionTestResult(
                isSuccess = false,
                latencyMs = latency,
                errorMessage = if (resp.statusCode == -1) "Connection refused at $baseUrl" else "HTTP ${resp.statusCode}"
            )
        }
    }
}
