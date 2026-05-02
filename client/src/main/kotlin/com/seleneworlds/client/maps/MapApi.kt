package com.seleneworlds.client.maps

import com.seleneworlds.client.tiles.TileApi
import com.seleneworlds.common.grid.Coordinate

class MapApi(private val clientMap: ClientMap) {

    fun getTilesAt(x: Int, y: Int, z: Int): List<TileApi> {
        return clientMap.getTilesAt(Coordinate(x, y, z)).map { it.api }
    }

    fun hasTileAt(x: Int, y: Int, z: Int): Boolean {
        return clientMap.hasTileAt(Coordinate(x, y, z))
    }
}
