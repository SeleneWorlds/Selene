package com.seleneworlds.client.grid

import com.seleneworlds.common.grid.GridApi
import com.seleneworlds.common.grid.Coordinate

class ClientGridApi(private val clientGrid: ClientGrid) : GridApi(clientGrid) {
    fun screenToCoordinate(x: Float, y: Float, z: Int): Coordinate = clientGrid.screenToCoordinate(x, y, z)
}
