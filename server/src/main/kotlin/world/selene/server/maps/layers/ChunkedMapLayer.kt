package world.selene.server.maps.layers

import world.selene.common.grid.Coordinate
import world.selene.server.maps.MapChunk

interface ChunkedMapLayer {
    val chunks: Map<Coordinate, MapChunk>
}