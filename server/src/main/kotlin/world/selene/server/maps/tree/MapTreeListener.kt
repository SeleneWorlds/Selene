package world.selene.server.maps.tree

import world.selene.common.grid.Coordinate

interface MapTreeListener {
    fun onTileUpdated(coordinate: Coordinate)
}
