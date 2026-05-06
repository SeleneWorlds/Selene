package com.seleneworlds.server.sync

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object ScopedChunkViewLuaApi {

    private fun getBaseTileAtRelative(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val ox = lua.checkInt(2)
        val oy = lua.checkInt(3)
        lua.push(chunkView.getBaseTileAtRelative(ox, oy))
        return 1
    }

    private fun getBaseTileAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getBaseTileAt(coordinate))
        return 1
    }

    private fun getAdditionalTilesAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getAdditionalTilesAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun getAnnotationsAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getAnnotationsAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun getAnnotationAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val key = lua.checkString(index + 1)
        lua.push(chunkView.getAnnotationAt(coordinate, key), Lua.Conversion.FULL)
        return 1
    }

    val luaMeta = LuaMappedMetatable(ScopedChunkViewApi::class) {
        callable(::getBaseTileAtRelative)
        callable(::getBaseTileAt)
        callable(::getAdditionalTilesAt)
        callable(::getAnnotationsAt)
        callable(::getAnnotationAt)
    }

}
