package world.selene.server.maps.layers

import world.selene.common.grid.Coordinate

interface BaseMapLayer : MapLayer {
    fun getTileId(coordinate: Coordinate): Int
}