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
    /** Returns whether a map tile exists at [coordinate], irrespective of whether it is passable. */
    fun hasTileAt(dimension: Dimension, viewer: Viewer, coordinate: Coordinate): Boolean {
        val chunkView = chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        if (registries.tiles.get(chunkView.getBaseTileAt(coordinate)) != null) {
            return true
        }
        return chunkView.getAdditionalTilesAt(coordinate).any { registries.tiles.get(it) != null }
    }

    fun collidesAt(
        dimension: Dimension,
        viewer: Viewer,
        coordinate: Coordinate,
        allowEmptyBase: Boolean = false
    ): Boolean {
        val chunkView = chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        var passable = true
        val baseTile = registries.tiles.get(chunkView.getBaseTileAt(coordinate))
        if ((baseTile == null && !allowEmptyBase) || baseTile?.impassable == true) {
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
        val entities = dimension.getEntitiesAt(coordinate)
        entities.forEach { entity ->
            if (entity.impassable) {
                passable = false
            } else if (entity.passableAbove) {
                passable = true
            }
        }
        return !passable
    }
}
