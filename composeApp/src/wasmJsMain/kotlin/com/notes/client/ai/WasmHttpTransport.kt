package com.notes.client.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WasmHttpTransport : HttpTransport {
    override suspend fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): HttpResponseData {
        // Fallback response for development/wasm runtime
        return HttpResponseData(
            statusCode = 200,
            body = "{}"
        )
    }
}

actual fun createDefaultHttpTransport(): HttpTransport = WasmHttpTransport()
