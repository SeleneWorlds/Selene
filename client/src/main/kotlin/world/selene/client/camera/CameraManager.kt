package world.selene.client.camera

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.grid.ClientGrid
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.maps.ClientMap
import world.selene.common.util.Coordinate

class CameraManager(
    private val map: ClientMap,
    private val grid: ClientGrid,
    private val signals: ClientLuaSignals
) {
    val camera = OrthographicCamera().apply {
        setToOrtho(true)
    }

    var focusCoordinate = Coordinate.Zero
        set(value) {
            if (field != value) {
                field = value
                signals.cameraCoordinateChanged.emit { lua -> lua.push(field, Lua.Conversion.NONE); 1 }
            }
        }
    var viewportOffsetX = 0
    var viewportOffsetY = 0
    var viewportWidth = camera.viewportWidth.toInt()
    var viewportHeight = camera.viewportHeight.toInt()

    private var focusedEntityNetworkId: Int = -1
    val focusedEntity get() = map.getEntityByNetworkId(focusedEntityNetworkId)

    fun update(delta: Float) {
        if (focusedEntityNetworkId != -1) {
            focusedEntity?.let { entity ->
                val entityScreenX = grid.getScreenX(entity.position)
                val entityScreenY = grid.getScreenY(entity.position)
                camera.position.x = entityScreenX + viewportOffsetX
                camera.position.y = entityScreenY + viewportOffsetY
                focusCoordinate = entity.coordinate
            }
        }

        camera.update()
    }

    fun focusCamera(coordinate: Coordinate) {
        focusCoordinate = coordinate
        camera.position.x = grid.getScreenX(coordinate) + viewportOffsetX
        camera.position.y = grid.getScreenY(coordinate) + viewportOffsetY
        camera.update()
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportOffsetX = (Gdx.graphics.width - width) / 2
        viewportOffsetY = (Gdx.graphics.height - height) / 2
        camera.position.x = grid.getScreenX(focusCoordinate) + viewportOffsetX
        camera.position.y = grid.getScreenY(focusCoordinate) + viewportOffsetY
        camera.update()
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