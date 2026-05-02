package com.seleneworlds.server.maps.layers

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.maps.MapChunk

interface ChunkedMapLayer {
    val chunks: Map<Coordinate, MapChunk>
}