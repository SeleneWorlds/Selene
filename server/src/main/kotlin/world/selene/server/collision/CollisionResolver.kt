package world.selene.server.collision

import world.selene.common.util.Coordinate
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.sync.ChunkViewManager

class CollisionResolver(val registries: Registries, val chunkViewManager: ChunkViewManager) {
    fun collidesAt(dimension: Dimension, viewer: Viewer, coordinate: Coordinate): Boolean {
        val chunkView = chunkViewManager.atCoordinate(dimension, viewer, coordinate)
        var passable = true
        val baseTileName = registries.mappings.getName("tiles", chunkView.getBaseTileAt(coordinate))
        val baseTile = baseTileName?.let { registries.tiles.get(baseTileName) }
        if (baseTile == null || baseTile.impassable) {
            passable = false
        }
        chunkView.getAdditionalTilesAt(coordinate).forEach { tileId ->
            val tileName = registries.mappings.getName("tiles", tileId)
            val tile = tileName?.let { registries.tiles.get(tileName) }
            if (tile?.impassable == true) {
                passable = false
            } else if (tile?.passableAbove == true) {
                passable = true
            }
        }
        return !passable
    }
}