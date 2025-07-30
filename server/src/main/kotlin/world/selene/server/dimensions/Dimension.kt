package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.lua.checkJavaObject
import world.selene.server.maps.MapTree
import world.selene.server.sync.DimensionSyncManager

class Dimension(var mapTree: MapTree) {
    val luaProxy = DimensionLuaProxy(this)
    val syncManager = DimensionSyncManager()

    class DimensionLuaProxy(val delegate: Dimension) {
        val Map: MapTree.MapTreeLuaProxy get() = delegate.mapTree.luaProxy

        fun SetMap(lua: Lua): Int {
            val mapTree = lua.checkJavaObject<MapTree.MapTreeLuaProxy>(-1)
            delegate.mapTree = mapTree.delegate
            return 0
        }
    }
}