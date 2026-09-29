package com.notes.client.ai

class IosHttpTransport : HttpTransport {
    override suspend fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): HttpResponseData {
        return HttpResponseData(
            statusCode = 501,
            body = "HTTP transport not yet implemented for iOS"
        )
    }
}

actual fun createDefaultHttpTransport(): HttpTransport = IosHttpTransport()
