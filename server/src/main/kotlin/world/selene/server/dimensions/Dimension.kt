package world.selene.server.dimensions

import world.selene.common.tiles.TileDefinition
import world.selene.common.grid.Coordinate
import world.selene.common.script.ExposedApi
import world.selene.server.cameras.viewer.DefaultViewer
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.data.Registries
import world.selene.server.entities.Entity
import world.selene.server.maps.tree.MapTree
import world.selene.server.maps.tree.MapTreeListener
import world.selene.server.tiles.TransientTile
import world.selene.server.sync.DimensionSyncManager
import world.selene.server.world.World

class Dimension(val registries: Registries, val world: World) : MapTreeListener, ExposedApi<DimensionApi> {
    override val api = DimensionApi(this)
    var mapTree: MapTree = MapTree(registries).also { it.addListener(this) }
        set(value) {
            field.removeListener(this)
            field = value
            value.addListener(this)
        }
    val syncManager = DimensionSyncManager()

    override fun onTileUpdated(coordinate: Coordinate) {
        syncManager.tileUpdated(coordinate)
    }

    fun swapTile(
        coordinate: Coordinate,
        oldTileDef: TileDefinition,
        newTileDef: TileDefinition,
        layerName: String?
    ): TransientTile {
        return if (mapTree.swapTile(coordinate, oldTileDef, newTileDef, layerName)) {
            TransientTile(newTileDef.asReference, this, coordinate)
        } else {
            TransientTile(oldTileDef.asReference, this, coordinate)
        }
    }

    fun getEntitiesAt(coordinate: Coordinate): List<Entity> {
        return world.entityManager.getEntitiesAt(coordinate, this)
    }

    fun getEntitiesInRange(coordinate: Coordinate, range: Int): List<Entity> {
        return world.entityManager.getNearbyEntities(coordinate, this, range)
    }

    fun getAnnotationAt(coordinate: Coordinate, key: String, viewer: Viewer = DefaultViewer): Map<*, *>? {
        val chunkView = world.chunkViewManager.atCoordinate(this, viewer, coordinate)
        return chunkView.getAnnotationAt(coordinate, key)
    }

}
