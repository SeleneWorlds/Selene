package com.seleneworlds.server.cameras

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.dimensions.Dimension

interface CameraListener {
    fun cameraDimensionChanged(camera: Camera, oldDimension: Dimension?, dimension: Dimension?)
    fun cameraCoordinateChanged(camera: Camera, prev: Coordinate, value: Coordinate)
}