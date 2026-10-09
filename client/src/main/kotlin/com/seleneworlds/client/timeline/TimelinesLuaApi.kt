package com.seleneworlds.client.timeline

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkSerializedMap
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.serialization.toJsonElement
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import java.util.UUID
import com.seleneworlds.common.timeline.TimelinePlaybackOptions

class TimelinesLuaApi(private val player: TimelinePlayer) : LuaModule {
    override val name = "selene.timelines"

    override fun register(table: LuaValue) {
        table.register("playAt", ::playAt)
    }

    private fun playAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val timeline = lua.checkString(index + 1)
        val parameters = if (lua.isNoneOrNil(index + 2)) emptyMap() else lua.checkSerializedMap(index + 2)
        val values = parameters.toMutableMap().apply { put("position", coordinate) }
        val options = TimelinePlaybackOptions.from(
            if (lua.isNoneOrNil(index + 3)) emptyMap() else lua.checkSerializedMap(index + 3)
        )
        player.play(options.instanceId ?: UUID.randomUUID().toString(), timeline, values.toJsonElement(), options.transition)
        return 0
    }
}
