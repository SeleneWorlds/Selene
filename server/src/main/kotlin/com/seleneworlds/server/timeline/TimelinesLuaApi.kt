package com.seleneworlds.server.timeline

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkSerializedMap
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.common.timeline.TimelinePlaybackOptions
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class TimelinesLuaApi(private val api: TimelinesApi) : LuaModule {
    override val name = "selene.timelines"

    override fun register(table: LuaValue) {
        table.register("play", ::play)
        table.register("playAt", ::playAt)
        table.register("stop", ::stop)
        table.register("stopAll", ::stopAll)
        table.register("stopTag", ::stopTag)
        table.register("stopAt", ::stopAt)
        table.register("stopAllAt", ::stopAllAt)
        table.register("stopTagAt", ::stopTagAt)
    }

    private fun play(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val timeline = lua.checkString(2)
        val parameters = if (lua.isNoneOrNil(3)) emptyMap() else lua.checkSerializedMap(3)
        val tags = readTags(lua, 4)
        lua.push(api.play(player, timeline, parameters, tags, readOptions(lua, 5)))
        return 1
    }

    private fun playAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val timeline = lua.checkString(index + 1)
        val parameters = if (lua.isNoneOrNil(index + 2)) emptyMap() else lua.checkSerializedMap(index + 2)
        val tags = readTags(lua, index + 3)
        lua.push(api.playAt(coordinate, timeline, parameters = parameters, tags = tags, options = readOptions(lua, index + 4)))
        return 1
    }

    private fun stopAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        api.stopAt(coordinate, lua.checkString(index + 1))
        return 0
    }

    private fun stop(lua: Lua): Int {
        api.stop(lua.checkUserdata<PlayerApi>(1), lua.checkString(2))
        return 0
    }

    private fun stopAll(lua: Lua): Int {
        api.stopAll(lua.checkUserdata<PlayerApi>(1), lua.checkString(2))
        return 0
    }

    private fun stopTag(lua: Lua): Int {
        api.stopTag(lua.checkUserdata<PlayerApi>(1), lua.checkString(2))
        return 0
    }

    private fun stopAllAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        api.stopAllAt(coordinate, lua.checkString(index + 1))
        return 0
    }

    private fun stopTagAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        api.stopTagAt(coordinate, lua.checkString(index + 1))
        return 0
    }

    private fun readOptions(lua: Lua, index: Int): TimelinePlaybackOptions =
        TimelinePlaybackOptions.from(if (lua.isNoneOrNil(index)) emptyMap() else lua.checkSerializedMap(index))

    private fun readTags(lua: Lua, index: Int): List<String> {
        if (lua.isNoneOrNil(index)) return emptyList()
        val tags = lua.toAny(index) as? List<*>
            ?: throw IllegalArgumentException("Timeline tags must be an array of strings")
        return tags.map {
            it as? String ?: throw IllegalArgumentException("Timeline tags must be strings")
        }
    }
}
