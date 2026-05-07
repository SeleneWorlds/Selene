package com.seleneworlds.common.grid

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata

object DirectionLuaApi {
    private fun getName(lua: Lua): Int {
        val direction = lua.checkUserdata<Direction>(1)
        lua.push(direction.name)
        return 1
    }

    private fun getVector(lua: Lua): Int {
        val direction = lua.checkUserdata<Direction>(1)
        lua.push(direction.vector, Lua.Conversion.NONE)
        return 1
    }

    private fun getAngle(lua: Lua): Int {
        val direction = lua.checkUserdata<Direction>(1)
        lua.push(direction.angle)
        return 1
    }

    val luaMeta = LuaMappedMetatable(Direction::class) {
        getter(::getName, "name")
        getter(::getVector, "vector")
        getter(::getAngle, "angle")
        callable(::getName)
        callable(::getVector)
        callable(::getAngle)
    }
}
