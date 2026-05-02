package com.seleneworlds.client.camera

import com.badlogic.gdx.math.Vector3
import com.seleneworlds.common.grid.Coordinate

/**
 * Manage the camera and convert coordinates.
 */
class CameraApi(
    private val cameraManager: CameraManager
) {
    fun getCoordinate(): Coordinate {
        return cameraManager.focusCoordinate
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        cameraManager.setViewport(x, y, width, height)
    }

    fun screenToWorld(x: Float, y: Float): Vector3 {
        return cameraManager.unproject(x, y)
    }
}
