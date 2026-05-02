package com.seleneworlds.server.maps.layers

import com.seleneworlds.common.grid.Coordinate

interface BaseMapLayer : MapLayer {
    fun getTileId(coordinate: Coordinate): Int
}