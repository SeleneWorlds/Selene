package com.seleneworlds.server.dimensions

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.entities.EntityApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.spatial.CoordinateBox
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.tiles.TransientTile
import com.seleneworlds.server.tiles.TransientTileApi

class DimensionApi(val dimension: Dimension) {

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

    fun annotateTile(coordinate: Coordinate, key: String, data: SerializedMap?, layerName: String?) {
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

    fun getAllAnnotations(viewer: Viewer = DefaultViewer): Map<Coordinate, Map<String, SerializedMap>> {
        return dimension.mapTree.getAllAnnotations(viewer)
    }

    /** Returns annotations within an inclusive square range on the same floor. */
    fun getAnnotationsInRange(
        coordinate: Coordinate, range: Int, viewer: Viewer = DefaultViewer
    ): Map<Coordinate, Map<String, SerializedMap>> {
        return dimension.getAnnotationsInRange(coordinate, range, viewer)
    }

    fun getAnnotationsAt(coordinate: Coordinate, viewer: Viewer = DefaultViewer): Map<String, Map<*, *>> {
        return dimension.getAnnotationsAt(coordinate, viewer)
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String, viewer: Viewer = DefaultViewer): SerializedMap? {
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

    /** Returns entities in an inclusive axis-aligned box, in entity registration order. */
    fun getEntitiesInBox(center: Coordinate, halfExtents: Coordinate): List<EntityApi> =
        dimension.world.entityManager.getEntitiesInBox(center, dimension, halfExtents).map { it.api }

    /** Uses controlled entities, rather than camera positions, to locate connected players. */
    fun getPlayersInBox(center: Coordinate, halfExtents: Coordinate): List<PlayerApi> {
        val box = CoordinateBox(center, halfExtents)
        return dimension.world.playerManager.players.filter { player ->
            val entity = player.controlledEntity
            entity != null && entity.dimension === dimension && box.contains(entity.coordinate)
        }.map { it.api }
    }

    /** Short-circuits without constructing a player list. Invisible players are included. */
    fun hasPlayersInBox(center: Coordinate, halfExtents: Coordinate): Boolean {
        val box = CoordinateBox(center, halfExtents)
        return dimension.world.playerManager.players.any { player ->
            val entity = player.controlledEntity
            entity != null && entity.dimension === dimension && box.contains(entity.coordinate)
        }
    }

    fun getObserversInRange(coordinate: Coordinate, range: Int, zRange: Int = 0): List<PlayerApi> {
        require(range >= 0 && zRange >= 0) { "Camera ranges must be non-negative" }
        return dimension.world.playerManager.players.filter { player ->
            val camera = player.camera
            // Following entities may have moved since the camera's last update.
            val cameraDimension = if (camera.followEntity != null) camera.followEntity?.dimension else camera.dimension
            val position = camera.followEntity?.coordinate ?: camera.coordinate
            player.connectionState == Player.ConnectionState.READY && cameraDimension === dimension &&
                position.horizontalDistanceTo(coordinate) <= range &&
                kotlin.math.abs(position.z.toLong() - coordinate.z) <= zRange
        }.map { it.api }
    }
}
