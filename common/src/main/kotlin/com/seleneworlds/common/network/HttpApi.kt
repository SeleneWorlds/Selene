package com.seleneworlds.common.network

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import com.seleneworlds.common.util.Disposable

/**
 * Make HTTP web requests.
 */
class HttpApi(
    private val httpClient: HttpClient
) : Disposable {
    data class HttpResult(val status: Int, val body: String, val success: Boolean)

    fun post(url: String, body: Any?, headers: Map<String, Any> = emptyMap()): HttpResult {
        try {
            val response = runBlocking {
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    headers.forEach { (key, value) ->
                        header(key, value.toString())
                    }
                }
            }

            val responseBody = runBlocking { response.bodyAsText() }
            return HttpResult(response.status.value, responseBody, response.status.isSuccess())
        } catch (e: Exception) {
            return HttpResult(0, e.message ?: "Unknown error", false)
        }
    }

    override fun dispose() {
        httpClient.close()
    }
}
