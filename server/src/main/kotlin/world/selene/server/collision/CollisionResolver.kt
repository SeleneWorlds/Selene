package world.selene.server.collision

import world.selene.common.grid.Coordinate
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.sync.ChunkViewManager

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