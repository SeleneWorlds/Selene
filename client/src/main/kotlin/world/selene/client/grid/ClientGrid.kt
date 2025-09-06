package world.selene.client.grid

import com.badlogic.gdx.math.Vector3
import world.selene.common.grid.Grid
import world.selene.common.util.Coordinate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class ClientGrid : Grid() {
    var tileWidth = 76
    var tileStepX = tileWidth / 2f
    var tileHeight = 37
    var tileStepY = (tileHeight + 1) / 2f
    var tileStepZ = 6 * tileStepY
    var zSortScale = 500
    var rowSortScale = 50

    fun getScreenX(coordinate: Coordinate) = (coordinate.x + coordinate.y) * tileStepX
    fun getScreenY(coordinate: Coordinate) =
        (((coordinate.x - coordinate.y) * tileStepY) + (coordinate.z * tileStepZ))

    fun getScreenX(position: Vector3) = (position.x + position.y) * tileStepX
    fun getScreenY(position: Vector3) =
        (((position.x - position.y) * tileStepY) + (position.z * tileStepZ))

    fun getSortLayer(coordinate: Coordinate, sortLayerOffset: Int) =
        ((coordinate.x - coordinate.y - (coordinate.z * zSortScale)) * rowSortScale) - sortLayerOffset

    fun getSortLayer(position: Vector3, sortLayerOffset: Int) =
        ((floor(position.x) - ceil(position.y) - (floor(position.z) * zSortScale)).toInt() * rowSortScale) - sortLayerOffset

    fun screenToCoordinate(x: Float, y: Float): Coordinate {
        val isoX = (x / tileStepX + (y / tileStepY)) / 2
        val isoY = (x / tileStepX - (y / tileStepY)) / 2
        return Coordinate(isoX.roundToInt(), isoY.roundToInt(), 0)
    }

}