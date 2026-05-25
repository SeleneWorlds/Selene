package com.seleneworlds.common.lua

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.lua.util.checkIdentifier
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.util.register

class EventsLuaApi : LuaModule {
    override val name = "selene.event"

    private val events = mutableMapOf<Identifier, LuaEvent>()

    override fun register(table: LuaValue) {
        table.register("of", ::of)
    }

    private fun of(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        lua.push(events.getOrPut(identifier, ::LuaEvent), Lua.Conversion.NONE)
        return 1
    }
}
