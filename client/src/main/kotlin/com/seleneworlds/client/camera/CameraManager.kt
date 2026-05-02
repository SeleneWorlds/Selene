package com.seleneworlds.client.camera

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.maps.ClientMap
import com.seleneworlds.common.grid.Coordinate

class CameraManager(
    private val map: ClientMap,
    private val grid: ClientGrid
) {
    val camera = OrthographicCamera().apply {
        setToOrtho(false)
    }

    var focusCoordinate = Coordinate.Zero
        set(value) {
            if (field != value) {
                field = value
                ClientEvents.CameraCoordinateChanged.EVENT.invoker().cameraCoordinateChanged(field)
            }
        }
    var viewportX = 0
    var viewportY = 0
    var viewportWidth = camera.viewportWidth.toInt()
    var viewportHeight = camera.viewportHeight.toInt()
    private var hasCustomViewport = false

    private var focusedEntityNetworkId: Int = -1
    val focusedEntity get() = map.getEntityByNetworkId(focusedEntityNetworkId)

    fun update() {
        if (focusedEntityNetworkId != -1) {
            focusedEntity?.let { entity ->
                val entityScreenX = grid.getScreenX(entity.position)
                val entityScreenY = grid.getScreenY(entity.position)
                camera.position.x = entityScreenX
                camera.position.y = entityScreenY
                focusCoordinate = entity.coordinate
            }
        }

        camera.update()
    }

    fun focusCamera(coordinate: Coordinate) {
        focusCoordinate = coordinate
        camera.position.x = grid.getScreenX(coordinate)
        camera.position.y = grid.getScreenY(coordinate)
        camera.update()
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        hasCustomViewport = true
        viewportX = x
        viewportY = y
        viewportWidth = width
        viewportHeight = height
        applyViewport()
    }

    fun resize(width: Int, height: Int) {
        if (!hasCustomViewport) {
            viewportX = 0
            viewportY = 0
            viewportWidth = width
            viewportHeight = height
        }
        applyViewport()
    }

    private fun applyViewport() {
        camera.viewportWidth = viewportWidth.toFloat()
        camera.viewportHeight = viewportHeight.toFloat()
        camera.position.x = grid.getScreenX(focusCoordinate)
        camera.position.y = grid.getScreenY(focusCoordinate)
        camera.update()
    }

    fun applyRenderViewport() {
        Gdx.gl.glViewport(viewportX, getViewportBottom(), viewportWidth, viewportHeight)
    }

    fun unproject(screenX: Float, screenY: Float): Vector3 {
        return camera.unproject(
            Vector3(screenX, screenY, 0f),
            viewportX.toFloat(),
            getViewportBottom().toFloat(),
            viewportWidth.toFloat(),
            viewportHeight.toFloat()
        )
    }

    private fun getViewportBottom(): Int {
        return Gdx.graphics.height - viewportY - viewportHeight
    }

    fun focusEntity(networkId: Int) {
        focusedEntityNetworkId = networkId
    }

    fun isRegionVisible(rectangle: Rectangle): Boolean {
        return isRegionVisible(rectangle.x, rectangle.x + rectangle.width, rectangle.y, rectangle.y + rectangle.height)
    }

    fun isRegionVisible(left: Float, right: Float, bottom: Float, top: Float): Boolean {
        // TODO Could be further optimized by culling into the actual viewport area as set view setViewport
        val camLeft = camera.position.x - camera.viewportWidth / 2f
        val camRight = camera.position.x + camera.viewportWidth / 2f
        val camBottom = camera.position.y - camera.viewportHeight / 2f
        val camTop = camera.position.y + camera.viewportHeight / 2f
        return !(right < camLeft || left > camRight || top < camBottom || bottom > camTop)
    }

    fun isInsideInterior(): Boolean {
        return focusCoordinate.z < 0 || map.hasTileAt(focusCoordinate.up())
    }

}
