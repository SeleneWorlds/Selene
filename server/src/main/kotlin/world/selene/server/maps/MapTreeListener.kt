package world.selene.server.maps

import world.selene.common.util.Coordinate

interface MapTreeListener {
    fun onTileUpdated(coordinate: Coordinate)
}
