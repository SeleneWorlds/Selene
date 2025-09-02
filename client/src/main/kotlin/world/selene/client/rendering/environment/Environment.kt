package world.selene.client.rendering.environment

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import world.selene.client.camera.CameraManager
import world.selene.common.util.Coordinate

class Environment(val cameraManager: CameraManager) {

    private val surfaceOffsets = mutableMapOf<Coordinate, Float>()

    private var isInsideInterior = false
    private val interiorFadeSpeed = 20f
    private var interiorFadeAlpha = 1f

    fun update(delta: Float) {
        surfaceOffsets.clear()

        isInsideInterior = cameraManager.isInsideInterior()
        interiorFadeAlpha = if (isInsideInterior) {
            MathUtils.lerp(interiorFadeAlpha, 0f, delta * interiorFadeSpeed)
        } else {
            MathUtils.lerp(interiorFadeAlpha, 1f, delta * interiorFadeSpeed)
        }
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

    fun shouldRender(coordinate: Coordinate): Boolean {
        if (isInsideInterior && isAboveFocus(coordinate) && interiorFadeAlpha < 0.01f) {
            return false
        }
        // TODO cameraManager.isRegionVisible(getBounds(context, x, y))
        return true
    }

    fun getColor(coordinate: Coordinate): Color {
        val baseColor = Color.WHITE.cpy()
        if (coordinate.z > cameraManager.focusCoordinate.z) {
            baseColor.a = interiorFadeAlpha
        }
        return baseColor
    }

}