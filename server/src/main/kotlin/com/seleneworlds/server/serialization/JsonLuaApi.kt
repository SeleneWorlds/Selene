package com.seleneworlds.server.serialization

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.common.serialization.unwrap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** JSON encoding and decoding for Lua persistence code. */
class JsonLuaApi(private val json: Json) : LuaModule {
    override val name = "selene.json"

    override fun register(table: LuaValue) {
        table.register("encode", this::encode)
        table.register("decode", this::decode)
    }

    private fun encode(lua: Lua): Int {
        lua.push(json.encodeToString(JsonElement.serializer(), lua.toAny(1).toJsonElement()))
        return 1
    }

    private fun decode(lua: Lua): Int {
        val value = json.decodeFromString(JsonElement.serializer(), lua.checkString(1)).unwrap()
        lua.push(value, Lua.Conversion.FULL)
        return 1
    }
}
