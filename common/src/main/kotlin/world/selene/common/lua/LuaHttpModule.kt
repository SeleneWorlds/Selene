package world.selene.common.lua

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.util.Disposable

class LuaHttpModule(
    private val httpClient: HttpClient
) : LuaModule, Disposable {
    override val name = "selene.http"

    override fun register(table: LuaValue) {
        table.register("Post", this::luaPost)
    }

    private fun luaPost(lua: Lua): Int {
        val url = lua.checkString(1)
        val body = if (lua.isString(2)) {
            lua.checkString(2)
        } else {
            lua.toMap(2)
        }
        val headers = if (lua.isTable(3)) lua.toMap(3) else emptyMap<String, Any>()

        try {
            val response = runBlocking {
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    headers?.forEach { (key, value) ->
                        header(key.toString(), value.toString())
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
            lua.push("status")
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
