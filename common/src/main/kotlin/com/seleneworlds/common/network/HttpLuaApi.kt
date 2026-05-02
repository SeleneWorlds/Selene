package com.seleneworlds.common.network

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAnyMap
import com.seleneworlds.common.lua.util.toTypedMap
import com.seleneworlds.common.util.Disposable

/**
 * Make HTTP web requests.
 */
class HttpLuaApi(private val api: HttpApi) : LuaModule, Disposable {
    override val name = "selene.http"

    override fun register(table: LuaValue) {
        table.register("Post", ::luaPost)
    }

    private fun luaPost(lua: Lua): Int {
        val url = lua.checkString(1)
        val body = if (lua.isString(2)) lua.checkString(2) else lua.toAnyMap(2)
        val headers = lua.toTypedMap<String, Any>(3) ?: emptyMap()
        val result = api.post(url, body, headers)

        lua.createTable(0, 3)
        lua.push(result.status)
        lua.setField(-2, "status")
        lua.push(result.body)
        lua.setField(-2, "body")
        lua.push(result.success)
        lua.setField(-2, "success")
        return 1
    }

    override fun dispose() {
        api.dispose()
    }
}
