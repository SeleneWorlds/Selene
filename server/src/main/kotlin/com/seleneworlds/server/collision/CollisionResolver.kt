package com.seleneworlds.server.collision

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.sync.ChunkViewManager

class CollisionResolver(
    val registries: Registries,
    val chunkViewManager: ChunkViewManager
) {
    fun collidesAt(dimension: Dimension, viewer: Viewer, coordinate: Coordinate): Boolean {
        val chunkView = chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        var passable = true
        val baseTile = registries.tiles.get(chunkView.getBaseTileAt(coordinate))
        if (baseTile == null || baseTile.impassable) {
            passable = false
        }
        chunkView.getAdditionalTilesAt(coordinate).forEach { tileId ->
            val tile = registries.tiles.get(tileId)
            if (tile?.impassable == true) {
                passable = false
            } else if (tile?.passableAbove == true) {
                passable = true
            }
        }
        if (passable) {
            val entities = dimension.getEntitiesAt(coordinate)
            entities.forEach { entity ->
                if (entity.impassable) {
                    passable = false
                }
            }
        }
        return !passable
    }
}