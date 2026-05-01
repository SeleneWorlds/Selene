package world.selene.server.sync

import party.iroiro.luajava.Lua
import world.selene.common.grid.ChunkWindow
import world.selene.common.grid.Coordinate
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class ScopedChunkViewApi(val chunkView: ScopedChunkView) : LuaMetatableProvider {

    fun getWindow(): ChunkWindow {
        return chunkView.window
    }

    fun getBaseTileAtRelative(ox: Int, oy: Int): Int {
        return chunkView.getBaseTileAtRelative(ox, oy)
    }

    fun getBaseTileAt(coordinate: Coordinate): Int {
        return chunkView.getBaseTileAt(coordinate)
    }

    fun getAdditionalTilesAt(coordinate: Coordinate): List<Int> {
        return chunkView.getAdditionalTilesAt(coordinate)
    }

    fun getAnnotationsAt(coordinate: Coordinate): Map<String, Map<*, *>> {
        return chunkView.getAnnotationsAt(coordinate)
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String): Map<*, *>? {
        return chunkView.getAnnotationAt(coordinate, key)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return ScopedChunkViewLuaApi.luaMeta
    }

}
