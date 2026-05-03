package com.seleneworlds.client.camera

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.maps.ClientMap
import com.seleneworlds.client.window.WindowViewport
import com.seleneworlds.common.grid.Coordinate
import kotlin.math.round
import kotlin.math.roundToInt

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
    private var logicalViewportWidth = camera.viewportWidth.toInt()
    private var logicalViewportHeight = camera.viewportHeight.toInt()
    private var customViewportX = 0
    private var customViewportY = 0
    private var customViewportWidth = camera.viewportWidth.toInt()
    private var customViewportHeight = camera.viewportHeight.toInt()
    private var hasCustomViewport = false
    private var windowViewport = WindowViewport(
        logicalWidth = camera.viewportWidth.toInt(),
        logicalHeight = camera.viewportHeight.toInt(),
        screenX = 0,
        screenY = 0,
        screenWidth = camera.viewportWidth.toInt(),
        screenHeight = camera.viewportHeight.toInt()
    )

    private var focusedEntityNetworkId: Int = -1
    val focusedEntity get() = map.getEntityByNetworkId(focusedEntityNetworkId)

    fun update() {
        if (focusedEntityNetworkId != -1) {
            focusedEntity?.let { entity ->
                val entityScreenX = grid.getScreenX(entity.position)
                val entityScreenY = grid.getScreenY(entity.position)
                setCameraPosition(entityScreenX, entityScreenY)
                focusCoordinate = entity.coordinate
            }
        }

        camera.update()
    }

    fun focusCamera(coordinate: Coordinate) {
        focusCoordinate = coordinate
        setCameraPosition(grid.getScreenX(coordinate), grid.getScreenY(coordinate))
        camera.update()
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        hasCustomViewport = true
        customViewportX = x
        customViewportY = y
        customViewportWidth = width
        customViewportHeight = height
        resize(windowViewport)
    }

    fun resize(windowViewport: WindowViewport) {
        this.windowViewport = windowViewport
        if (hasCustomViewport) {
            logicalViewportWidth = customViewportWidth
            logicalViewportHeight = customViewportHeight

            val screenLeft = windowViewport.screenX + scaleLogicalX(customViewportX, windowViewport)
            val screenTop = windowViewport.screenY + scaleLogicalY(customViewportY, windowViewport)
            val screenRight = windowViewport.screenX + scaleLogicalX(customViewportX + customViewportWidth, windowViewport)
            val screenBottom = windowViewport.screenY + scaleLogicalY(customViewportY + customViewportHeight, windowViewport)

            viewportX = screenLeft
            viewportY = screenTop
            viewportWidth = screenRight - screenLeft
            viewportHeight = screenBottom - screenTop
        } else {
            viewportX = windowViewport.screenX
            viewportY = windowViewport.screenY
            viewportWidth = windowViewport.screenWidth
            viewportHeight = windowViewport.screenHeight
            logicalViewportWidth = windowViewport.logicalWidth
            logicalViewportHeight = windowViewport.logicalHeight
        }
        applyViewport()
    }

    private fun scaleLogicalX(value: Int, windowViewport: WindowViewport): Int {
        return (value.toFloat() * windowViewport.screenWidth.toFloat() / windowViewport.logicalWidth.toFloat()).roundToInt()
    }

    private fun scaleLogicalY(value: Int, windowViewport: WindowViewport): Int {
        return (value.toFloat() * windowViewport.screenHeight.toFloat() / windowViewport.logicalHeight.toFloat()).roundToInt()
    }

    private fun applyViewport() {
        camera.viewportWidth = logicalViewportWidth.toFloat()
        camera.viewportHeight = logicalViewportHeight.toFloat()
        setCameraPosition(grid.getScreenX(focusCoordinate), grid.getScreenY(focusCoordinate))
        camera.update()
    }

    private fun setCameraPosition(x: Float, y: Float) {
        camera.position.x = snapToPixelGrid(x, logicalViewportWidth)
        camera.position.y = snapToPixelGrid(y, logicalViewportHeight)
    }

    private fun snapToPixelGrid(value: Float, viewportSize: Int): Float {
        val viewportOffset = if (viewportSize % 2 == 0) 0f else 0.5f
        return round(value - viewportOffset) + viewportOffset
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
