package world.selene.common.network

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.toAnyMap
import world.selene.common.lua.util.toTypedMap
import world.selene.common.util.Disposable

/**
 * Make HTTP web requests.
 */
@Suppress("SameReturnValue")
class HttpApi(
    private val httpClient: HttpClient
) : Disposable {
    fun luaPost(lua: Lua): Int {
        val url = lua.checkString(1)
        val body = if (lua.isString(2)) {
            lua.checkString(2)
        } else {
            lua.toAnyMap(2)
        }
        val headers = lua.toTypedMap<String, Any>(3) ?: emptyMap()

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
            val statusCode = response.status.value
            lua.createTable(0, 3)
            lua.push(statusCode)
            lua.setField(-2, "status")
            lua.push(responseBody)
            lua.setField(-2, "body")
            lua.push(response.status.isSuccess())
            lua.setField(-2, "success")
        } catch (e: Exception) {
            lua.createTable(0, 3)
            lua.push(0)
            lua.setField(-2, "status")
            lua.push(e.message ?: "Unknown error")
            lua.setField(-2, "body")
            lua.push(false)
            lua.setField(-2, "success")
        }
        return 1
    }

    override fun dispose() {
        httpClient.close()
    }
}
