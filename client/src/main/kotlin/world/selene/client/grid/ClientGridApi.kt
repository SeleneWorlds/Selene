package world.selene.client.grid

import world.selene.common.grid.GridApi
import world.selene.common.grid.Coordinate

class ClientGridApi(private val clientGrid: ClientGrid) : GridApi(clientGrid) {
    fun screenToCoordinate(x: Float, y: Float, z: Int): Coordinate = clientGrid.screenToCoordinate(x, y, z)
}
