package com.notes.client.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
 * Default standard HTTP transport utilizing HttpURLConnection on Android/JVM platforms.
 */
class DefaultHttpTransport : HttpTransport {

    override suspend fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Long
    ): HttpResponseData = withContext(Dispatchers.IO) {
        val targetUrl = URL(url)
        val conn = (targetUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs.toInt()
            readTimeout = timeoutMs.toInt()
            doInput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        if (!body.isNullOrEmpty() && (method == "POST" || method == "PUT" || method == "PATCH")) {
            conn.doOutput = true
            conn.outputStream.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }
        }

        val statusCode = try {
            conn.responseCode
        } catch (e: Exception) {
            -1
        }

        val inputStream = if (statusCode in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream ?: conn.inputStream
        }

        val responseBody = try {
            inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        } catch (_: Exception) {
            ""
        } finally {
            conn.disconnect()
        }

        HttpResponseData(
            statusCode = statusCode,
            body = responseBody
        )
    }
}
