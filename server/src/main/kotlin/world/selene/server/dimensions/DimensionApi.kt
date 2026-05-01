package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.grid.Coordinate
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.server.cameras.viewer.DefaultViewer
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.entities.EntityApi
import world.selene.server.maps.tree.MapTreeApi
import world.selene.server.tiles.TransientTile
import world.selene.server.tiles.TransientTileApi

class DimensionApi(val dimension: Dimension) : LuaMetatableProvider {

    fun getMapTree(): MapTreeApi {
        return dimension.mapTree.api
    }

    fun setMapTree(mapTree: MapTreeApi) {
        dimension.mapTree = mapTree.mapTree
    }

    fun hasTile(coordinate: Coordinate, tileId: Int, viewer: Viewer = DefaultViewer): Boolean {
        val chunkView = dimension.world.chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        val baseTile = chunkView.getBaseTileAt(coordinate)
        return baseTile == tileId || chunkView.getAdditionalTilesAt(coordinate).contains(tileId)
    }

    fun placeTile(coordinate: Coordinate, tileDefId: Int, layerName: String?): TransientTileApi {
        val tileDef = dimension.registries.tiles.get(tileDefId)!!
        dimension.mapTree.placeTile(coordinate, tileDef, layerName)
        return TransientTile(tileDef.asReference, dimension, coordinate).api
    }

    fun annotateTile(coordinate: Coordinate, key: String, data: Map<Any, Any>?, layerName: String?) {
        dimension.mapTree.annotateTile(coordinate, key, data, layerName)
    }

    fun getTilesAt(coordinate: Coordinate, viewer: Viewer = DefaultViewer): List<TransientTileApi> {
        val tiles = mutableListOf<TransientTileApi>()
        val chunkView = dimension.world.chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        val baseTileId = chunkView.getBaseTileAt(coordinate)
        val baseTile = dimension.registries.tiles.get(baseTileId)
        if (baseTile != null) {
            tiles.add(TransientTile(baseTile.asReference, dimension, coordinate).api)
        }
        val additionalTiles = chunkView.getAdditionalTilesAt(coordinate)
        additionalTiles.forEach { tileId ->
            val tile = dimension.registries.tiles.get(tileId)
            if (tile != null) {
                tiles.add(TransientTile(tile.asReference, dimension, coordinate).api)
            }
        }
        return tiles
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String, viewer: Viewer = DefaultViewer): Map<*, *>? {
        return dimension.getAnnotationAt(coordinate, key, viewer)
    }

    fun hasCollisionAt(coordinate: Coordinate, viewer: Viewer = DefaultViewer): Boolean {
        return dimension.world.collisionResolver.collidesAt(dimension, viewer, coordinate)
    }

    fun getEntitiesAt(coordinate: Coordinate): List<EntityApi> {
        return dimension.getEntitiesAt(coordinate).map { it.api }
    }

    fun getEntitiesInRange(coordinate: Coordinate, range: Int): List<EntityApi> {
        return dimension.getEntitiesInRange(coordinate, range).map { it.api }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return DimensionLuaApi.luaMeta
    }

}
