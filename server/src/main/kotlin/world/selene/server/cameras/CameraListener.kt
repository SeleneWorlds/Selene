package world.selene.server.cameras

import world.selene.common.grid.Coordinate
import world.selene.server.dimensions.Dimension

interface CameraListener {
    fun cameraDimensionChanged(camera: Camera, oldDimension: Dimension?, dimension: Dimension?)
    fun cameraCoordinateChanged(camera: Camera, prev: Coordinate, value: Coordinate)
}