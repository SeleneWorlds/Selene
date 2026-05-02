package com.seleneworlds.server.maps.tree

import com.seleneworlds.common.grid.Coordinate

interface MapTreeListener {
    fun onTileUpdated(coordinate: Coordinate)
}
