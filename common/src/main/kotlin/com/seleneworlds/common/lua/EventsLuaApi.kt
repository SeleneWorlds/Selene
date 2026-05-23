package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.util.register

class EventsLuaApi : LuaModule {
    override val name = "selene.events"

    override fun register(table: LuaValue) {
        table.register("create", ::create)
    }

    private fun create(lua: Lua): Int {
        lua.push(LuaEvent(), Lua.Conversion.NONE)
        return 1
    }
}
