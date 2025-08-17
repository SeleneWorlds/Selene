package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkJavaObject
import world.selene.server.cameras.DefaultViewer
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.maps.MapTree
import world.selene.server.maps.TileLuaProxy
import world.selene.server.sync.ChunkViewManager
import world.selene.server.sync.DimensionSyncManager

class Dimension(val registries: Registries, val chunkViewManager: ChunkViewManager) {
    var mapTree: MapTree = MapTree(registries)
    val luaProxy = DimensionLuaProxy(this)
    val syncManager = DimensionSyncManager()

    class DimensionLuaProxy(val delegate: Dimension) {
        val Map: MapTree.MapTreeLuaProxy get() = delegate.mapTree.luaProxy

        fun SetMap(lua: Lua): Int {
            val mapTree = lua.checkJavaObject<MapTree.MapTreeLuaProxy>(-1)
            delegate.mapTree = mapTree.delegate
            return 0
        }

        fun GetTilesAt(lua: Lua): Int {
            val (coordinate, index) = lua.checkCoordinate(2)
            val viewer = if (lua.isUserdata(index)) lua.checkJavaObject<Viewer>(index) else DefaultViewer

            try {
                val tiles = mutableListOf<TileLuaProxy>()
                val chunkView = delegate.chunkViewManager.atCoordinate(delegate, viewer, coordinate)
                val baseTile = chunkView.getBaseTileAt(coordinate)
                val baseTileName = delegate.registries.mappings.getName("tiles", baseTile)
                val baseTileDef = baseTileName?.let { delegate.registries.tiles.get(it) }
                if (baseTileDef != null) {
                    tiles.add(TileLuaProxy(baseTileName, baseTileDef, this, coordinate))
                }
                val additionalTiles = chunkView.getAdditionalTilesAt(coordinate)
                additionalTiles.forEach { tileId ->
                    val tileName = delegate.registries.mappings.getName("tiles", tileId)
                    val tileDef = tileName?.let { delegate.registries.tiles.get(it) }
                    if (tileDef != null) {
                        tiles.add(TileLuaProxy(tileName, tileDef, this, coordinate))
                    }
                }
                lua.push(tiles, Lua.Conversion.FULL)
                return 1
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to get tiles at $coordinate: ${e.message}", e))
            }
        }
    }
}

