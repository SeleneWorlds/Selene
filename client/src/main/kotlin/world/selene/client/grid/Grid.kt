package world.selene.client.grid

import com.badlogic.gdx.math.Vector3
import world.selene.common.util.Coordinate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class Grid {
    var tileWidth = 76
    var tileStepX = tileWidth / 2f
    var tileHeight = 37
    var tileStepY = (tileHeight + 1) / 2f
    var tileStepZ = 6 * tileStepY
    var zSortScale = 500
    var rowSortScale = 50

    data class Direction(val name: String, val vector: Coordinate) {
        companion object {
            val None = Direction("none", Coordinate(0, 0, 0))
        }
    }

    val directions = mutableMapOf<String, Direction>()

    fun getScreenX(coordinate: Coordinate) = (coordinate.x + coordinate.y) * tileStepX
    fun getScreenY(coordinate: Coordinate) =
        -(((coordinate.x - coordinate.y) * tileStepY) + (coordinate.z * tileStepZ))

    fun getScreenX(position: Vector3) = (position.x + position.y) * tileStepX
    fun getScreenY(position: Vector3) =
        -(((position.x - position.y) * tileStepY) + (position.z * tileStepZ))

    fun getSortLayer(coordinate: Coordinate, sortLayerOffset: Int) =
        ((coordinate.x - coordinate.y - (coordinate.z * zSortScale)) * rowSortScale) - sortLayerOffset
    fun getSortLayer(position: Vector3, sortLayerOffset: Int) =
        ((floor(position.x) - ceil(position.y) - (floor(position.z) * zSortScale)).toInt() * rowSortScale) - sortLayerOffset

    fun defineDirection(name: String, direction: Coordinate): Coordinate {
        directions[name] = Direction(name, direction)
        return direction
    }

    fun screenToCoordinate(x: Float, y: Float): Coordinate {
        val isoX = (x / tileStepX + (-y / tileStepY)) / 2
        val isoY = (x / tileStepX - (-y / tileStepY)) / 2
        return Coordinate(isoX.roundToInt(), isoY.roundToInt(), 0)
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
}