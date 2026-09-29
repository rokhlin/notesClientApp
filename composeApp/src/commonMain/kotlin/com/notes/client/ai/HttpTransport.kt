package com.notes.client.ai

data class HttpResponseData(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)

interface HttpTransport {
    suspend fun execute(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        timeoutMs: Long = 15_000L
    ): HttpResponseData
}

/**
 * Creates the platform-specific default HTTP transport.
 */
expect fun createDefaultHttpTransport(): HttpTransport

/**
 * Backward-compatible factory function matching previous constructor syntax.
 */
fun DefaultHttpTransport(): HttpTransport = createDefaultHttpTransport()
