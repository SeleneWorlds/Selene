package com.seleneworlds.common.grid

open class Grid {
    var layout = GridLayout.DIAMOND
        private set

    val directions = mutableMapOf<String, Direction>()

    fun clearDirections() {
        directions.clear()
    }

    fun applyDefinition(definition: GridDefinition) {
        layout = definition.layout
        clearDirections()
        definition.directions.forEach { direction ->
            defineDirection(direction.name, Coordinate(direction.x, direction.y, direction.z), direction.angle)
        }
    }

    fun defineDirection(name: String, direction: Coordinate, angle: Float): Coordinate {
        directions[name] = Direction(name, direction, angle)
        return direction
    }

    fun getDirection(angle: Float): Direction {
        return directions.values.minByOrNull { dir ->
            val ddx = dir.angle - angle
            ddx * ddx
        } ?: Direction.None
    }

    fun getDirection(from: Coordinate, to: Coordinate): Direction? {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        val delta = Coordinate(dx, dy, dz)
        return directions.values.minByOrNull { dir ->
            val ddx = delta.x - dir.vector.x
            val ddy = delta.y - dir.vector.y
            val ddz = delta.z - dir.vector.z
            ddx * ddx + ddy * ddy + ddz * ddz
        }
    }

    /**
     * Returns whether [to] is exactly one configured, non-zero grid step from [from].
     * Differences are calculated as longs so coordinates near Int boundaries cannot wrap
     * around and masquerade as a neighboring tile.
     */
    fun isAllowedStep(from: Coordinate, to: Coordinate): Boolean {
        val dx = to.x.toLong() - from.x.toLong()
        val dy = to.y.toLong() - from.y.toLong()
        val dz = to.z.toLong() - from.z.toLong()
        return directions.values.any { direction ->
            val vector = direction.vector
            vector != Coordinate.Zero &&
                dx == vector.x.toLong() &&
                dy == vector.y.toLong() &&
                dz == vector.z.toLong()
        }
    }

    fun getDirectionByName(name: String): Direction? {
        return directions[name]
    }
}
