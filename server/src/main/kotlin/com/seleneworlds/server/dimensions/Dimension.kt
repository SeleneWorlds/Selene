package com.seleneworlds.server.dimensions

import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.maps.tree.MapTree
import com.seleneworlds.server.maps.tree.MapTreeListener
import com.seleneworlds.server.tiles.TransientTile
import com.seleneworlds.server.sync.DimensionSyncManager
import com.seleneworlds.server.world.World

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

    fun getAnnotationAt(coordinate: Coordinate, key: String, viewer: Viewer = DefaultViewer): SerializedMap? {
        val chunkView = world.chunkViewManager.atCoordinate(this, viewer, coordinate)
        return chunkView.getAnnotationAt(coordinate, key)
    }

}
