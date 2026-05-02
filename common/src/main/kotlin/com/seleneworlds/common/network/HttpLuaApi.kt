package com.seleneworlds.common.network

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkSerializedMap
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.throwTypeError
import com.seleneworlds.common.lua.util.toSerializedMap
import com.seleneworlds.common.util.Disposable
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

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
        val body = when(val type = lua.type(2)) {
            Lua.LuaType.STRING -> lua.checkString(2)
            Lua.LuaType.TABLE -> lua.checkSerializedMap(2)
            Lua.LuaType.NIL -> null
            else -> lua.throwTypeError(2, Lua.LuaType.STRING, type)
        }
        val headers = if (!lua.isNil(3)) lua.checkSerializedMap(3) else emptyMap()
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
