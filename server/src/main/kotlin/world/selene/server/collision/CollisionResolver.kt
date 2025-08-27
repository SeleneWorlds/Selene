package world.selene.server.collision

import world.selene.common.util.Coordinate
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.EntityManager
import world.selene.server.sync.ChunkViewManager
import world.selene.server.world.World

class CollisionResolver(
    val registries: Registries,
    val chunkViewManager: ChunkViewManager,
    val entityManager: EntityManager
) {
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