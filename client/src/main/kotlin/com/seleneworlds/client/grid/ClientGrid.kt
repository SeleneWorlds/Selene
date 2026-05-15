package com.seleneworlds.client.grid

import com.badlogic.gdx.math.Vector3
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.GridLayout
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class ClientGrid : Grid() {
    var tileWidth = 76
        private set
    var tileStepX = 38f
        private set
    var tileHeight = 37
        private set
    var tileStepY = 19f
        private set
    var tileStepZ = 114f
        private set
    var zSortScale = 500
        private set
    var rowSortScale = 50
        private set

    fun applyDefinition(definition: RenderGridDefinition) {
        tileWidth = definition.tileWidth
        tileHeight = definition.tileHeight
        tileStepX = definition.tileStepX
        tileStepY = definition.tileStepY
        tileStepZ = definition.tileStepZ
        zSortScale = definition.zSortScale
        rowSortScale = definition.rowSortScale
    }

    fun getScreenX(coordinate: Coordinate) = when (layout) {
        GridLayout.DIAMOND -> (coordinate.x + coordinate.y) * tileStepX
    }

    fun getScreenY(coordinate: Coordinate) = when (layout) {
        GridLayout.DIAMOND -> ((coordinate.x - coordinate.y) * tileStepY) + (coordinate.z * tileStepZ)
    }

    fun getScreenX(position: Vector3) = when (layout) {
        GridLayout.DIAMOND -> (position.x + position.y) * tileStepX
    }

    fun getScreenY(position: Vector3) = when (layout) {
        GridLayout.DIAMOND -> ((position.x - position.y) * tileStepY) + (position.z * tileStepZ)
    }

    fun getSortLayer(coordinate: Coordinate, sortLayerOffset: Int) = when (layout) {
        GridLayout.DIAMOND ->
            ((coordinate.x - coordinate.y - (coordinate.z * zSortScale)) * rowSortScale) - sortLayerOffset
    }

    fun getSortLayer(position: Vector3, sortLayerOffset: Int) = when (layout) {
        GridLayout.DIAMOND ->
            ((floor(position.x) - ceil(position.y) - (floor(position.z) * zSortScale)).toInt() * rowSortScale) - sortLayerOffset
    }

    fun screenToCoordinate(x: Float, y: Float, z: Int = 0): Coordinate {
        return when (layout) {
            GridLayout.DIAMOND -> {
                val adjustedY = y - (z * tileStepZ)
                val isoX = (x / tileStepX + (adjustedY / tileStepY)) / 2
                val isoY = (x / tileStepX - (adjustedY / tileStepY)) / 2
                Coordinate(isoX.roundToInt(), isoY.roundToInt(), z)
            }
        }
    }
}
