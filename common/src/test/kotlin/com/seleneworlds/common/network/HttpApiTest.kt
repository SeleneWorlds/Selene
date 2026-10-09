package com.seleneworlds.common.network

import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpApiTest {
    @Test
    fun `post preserves multipart body and uses one explicit content type while retaining JSON default`() {
        val requests = LinkedBlockingQueue<Pair<List<String>, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(exchange.requestHeaders["Content-Type"].orEmpty() to
                exchange.requestBody.readAllBytes().toString(Charsets.UTF_8))
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
        val api = HttpApi(HttpClient(CIO))
        try {
            val url = "http://127.0.0.1:${server.address.port}/"
            val contentType = "multipart/form-data; boundary=TestBoundary"
            val body = "--TestBoundary\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\"character.json\"\r\n\r\n{\"name\":\"Test ä\"}\r\n--TestBoundary--\r\n"
            assertTrue(api.post(url, body, mapOf("content-type" to contentType)).success)
            val multipart = requests.poll(5, TimeUnit.SECONDS)!!
            assertEquals(listOf(contentType), multipart.first)
            assertEquals(body, multipart.second)
            assertTrue(api.post(url, "{}").success)
            val json = requests.poll(5, TimeUnit.SECONDS)!!
            assertEquals(listOf("application/json"), json.first)
            assertEquals("{}", json.second)
        } finally {
            api.dispose()
            server.stop(0)
        }
    }
}
