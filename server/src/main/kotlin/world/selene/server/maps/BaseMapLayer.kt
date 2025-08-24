package world.selene.server.maps

import world.selene.common.util.Coordinate

interface BaseMapLayer : MapLayer {
    fun getTileId(coordinate: Coordinate): Int
}