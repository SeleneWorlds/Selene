package world.selene.client.rendering.environment

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import world.selene.client.camera.CameraManager
import world.selene.client.grid.ClientGrid
import world.selene.common.grid.Coordinate

class Environment(val cameraManager: CameraManager, val grid: ClientGrid) {

    private val surfaceOffsets = mutableMapOf<Coordinate, Float>()

    private var isInsideInterior = false
    private val interiorFadeSpeed = 20f
    private var interiorFadeAlpha = 1f

    private val focusBounds = Rectangle()

    fun update(delta: Float) {
        surfaceOffsets.clear()

        isInsideInterior = cameraManager.isInsideInterior()
        interiorFadeAlpha = if (isInsideInterior) {
            MathUtils.lerp(interiorFadeAlpha, 0f, delta * interiorFadeSpeed)
        } else {
            MathUtils.lerp(interiorFadeAlpha, 1f, delta * interiorFadeSpeed)
        }

        val focusCoordinate = cameraManager.focusCoordinate
        cameraManager.focusedEntity?.lastRenderBounds?.let { focusBounds.set(it) } ?: focusBounds.set(
            grid.getScreenX(focusCoordinate), grid.getScreenY(focusCoordinate), 1f, 1f
        )
    }

    fun getSurfaceOffset(coordinate: Coordinate): Float {
        return surfaceOffsets.getOrDefault(coordinate, 0f)
    }

    fun applySurfaceOffset(coordinate: Coordinate, surfaceHeight: Float) {
        surfaceOffsets.compute(coordinate) { _, value ->
            (value ?: 0f) + surfaceHeight
        }
    }

    private fun isAboveFocus(coordinate: Coordinate): Boolean {
        return coordinate.z > cameraManager.focusCoordinate.z
    }

    fun shouldRender(coordinate: Coordinate, bounds: Rectangle): Boolean {
        if (isInsideInterior && isAboveFocus(coordinate) && interiorFadeAlpha < 0.01f) {
            return false
        }
        return cameraManager.isRegionVisible(bounds)
    }

    fun getColor(coordinate: Coordinate): Color {
        val baseColor = Color.WHITE.cpy()
        if (coordinate.z > cameraManager.focusCoordinate.z) {
            baseColor.a = interiorFadeAlpha
        }
        return baseColor
    }

    fun occludesFocus(coordinate: Coordinate, bounds: Rectangle): Boolean {
        val focusCoordinate = cameraManager.focusCoordinate
        val occluderSortLayer = grid.getSortLayer(coordinate, 0)
        val focusSortLayer = grid.getSortLayer(focusCoordinate, 0)
        if (focusSortLayer - occluderSortLayer < grid.rowSortScale) {
            return false
        }

        val isLargerThanFocus = bounds.height >= focusBounds.height
        val isAboveFocus = focusCoordinate.z < coordinate.z
        return (isLargerThanFocus || isAboveFocus) && bounds.overlaps(focusBounds)
    }

}