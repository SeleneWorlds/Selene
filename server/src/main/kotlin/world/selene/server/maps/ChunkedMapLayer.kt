package world.selene.server.maps

import world.selene.common.util.Coordinate

interface ChunkedMapLayer {
    val chunks: Map<Coordinate, MapChunk>
}