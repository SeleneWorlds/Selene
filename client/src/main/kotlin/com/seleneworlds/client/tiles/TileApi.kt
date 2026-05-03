package com.seleneworlds.client.tiles

import com.seleneworlds.client.rendering.visual.ReloadableVisualApi
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.tiles.TileDefinition

class TileApi(val tile: Tile) {

    fun getCoordinate(): Coordinate {
        return tile.coordinate
    }

    fun getDefinition(): RegistryReference<TileDefinition> {
        return tile.tileDefinition
    }

    fun getVisual(): ReloadableVisualApi {
        return tile.visual.api
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

    fun getName(): String {
        return tile.tileDefinition.identifier.toString()
    }

    override fun toString(): String {
        return "TileApi(${tile})"
    }

}
