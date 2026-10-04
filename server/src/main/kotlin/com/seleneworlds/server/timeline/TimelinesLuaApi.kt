package com.seleneworlds.server.timeline

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkSerializedMap
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class TimelinesLuaApi(private val api: TimelinesApi) : LuaModule {
    override val name = "selene.timelines"

    override fun register(table: LuaValue) {
        table.register("playAt", ::playAt)
    }

    private fun playAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val timeline = lua.checkString(index + 1)
        val parameters = if (lua.isNoneOrNil(index + 2)) emptyMap() else lua.checkSerializedMap(index + 2)
        api.playAt(coordinate, timeline, parameters = parameters)
        return 0
    }
}
