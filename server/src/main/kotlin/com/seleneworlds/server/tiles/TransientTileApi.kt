package com.seleneworlds.server.tiles

import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.server.dimensions.DimensionApi

class TransientTileApi(val tile: TransientTile) {

    fun getDefinition(): RegistryReference<TileDefinition> {
        return tile.definition
    }

    fun getName(): String {
        return tile.identifier.toString()
    }

    fun getDimension(): DimensionApi {
        return tile.dimension.api
    }

    fun getCoordinate(): Coordinate {
        return tile.coordinate
    }

    fun getX(): Int {
        return tile.x
    }

    fun getY(): Int {
        return tile.y
    }

    fun getZ(): Int {
        return tile.z
    }

    fun getMetadata(key: String): Any? {
        return tile.definition.metadata[key]
    }

    fun hasTag(tag: String): Boolean {
        return tile.definition.tags.contains(tag)
    }

    fun swap(newTileDef: TileDefinition, layerName: String?): TransientTileApi {
        val oldTileDef = tile.definition.get()
            ?: error("Tried to swap tile at ${tile.coordinate} but tile definition ${tile.definition.identifier} is not valid")
        return tile.dimension.swapTile(tile.coordinate, oldTileDef, newTileDef, layerName).api
    }
}
