package world.selene.common.grid

import world.selene.common.lua.LuaMetatable
import kotlin.math.sqrt

/**
 * Location in the tile grid.
 *
 * Can also be passed as three x, y, and z arguments or a table with x, y, and z fields.
 */
data class Coordinate(val x: Int, val y: Int, val z: Int) {
    fun horizontalDistanceTo(other: Coordinate): Int {
        val dx = (x - other.x).toFloat()
        val dy = (y - other.y).toFloat()
        return sqrt(dx * dx + dy * dy).toInt()
    }

    operator fun plus(other: Coordinate): Coordinate {
        return Coordinate(x + other.x, y + other.y, z + other.z)
    }

    fun up(): Coordinate {
        return Coordinate(x, y, z + 1)
    }

    companion object {
        val Zero = Coordinate(0, 0, 0)
    }
}
