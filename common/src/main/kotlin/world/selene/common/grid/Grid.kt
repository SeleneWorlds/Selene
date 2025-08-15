package world.selene.common.grid

import world.selene.common.util.Coordinate

open class Grid {
    data class Direction(val name: String, val vector: Coordinate, val angle: Float = 0f) {
        companion object {
            val None = Direction("none", Coordinate(0, 0, 0), 0f)
        }
    }

    val directions = mutableMapOf<String, Direction>()

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

    fun getDirectionByName(name: String): Direction? {
        return directions[name]
    }
}