package com.seleneworlds.common.grid

open class GridApi(private val grid: Grid) {

    fun getDirectionByName(name: String): Direction {
        return grid.getDirectionByName(name)
            ?: throw IllegalArgumentException("Unknown direction: $name")
    }

    fun defineDirection(name: String, x: Int, y: Int, z: Int, angle: Float): Coordinate {
        return grid.defineDirection(name, Coordinate(x, y, z), angle)
    }
}
