package world.selene.server.sync

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

object ScopedChunkViewLuaApi {

    private fun luaGetBaseTileAtRelative(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val ox = lua.checkInt(2)
        val oy = lua.checkInt(3)
        lua.push(chunkView.getBaseTileAtRelative(ox, oy))
        return 1
    }

    private fun luaGetBaseTileAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getBaseTileAt(coordinate))
        return 1
    }

    private fun luaGetAdditionalTilesAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getAdditionalTilesAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetAnnotationsAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(chunkView.getAnnotationsAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetAnnotationAt(lua: Lua): Int {
        val chunkView = lua.checkUserdata<ScopedChunkViewApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val key = lua.checkString(index + 1)
        lua.push(chunkView.getAnnotationAt(coordinate, key), Lua.Conversion.FULL)
        return 1
    }

    val luaMeta = LuaMappedMetatable(ScopedChunkViewApi::class) {
        callable(::luaGetBaseTileAtRelative)
        callable(::luaGetBaseTileAt)
        callable(::luaGetAdditionalTilesAt)
        callable(::luaGetAnnotationsAt)
        callable(::luaGetAnnotationAt)
    }

}
