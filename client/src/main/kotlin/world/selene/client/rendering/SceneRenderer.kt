package world.selene.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import world.selene.client.camera.CameraManager
import world.selene.client.grid.Grid
import world.selene.client.scene.Scene
import world.selene.client.visual.VisualContext
import world.selene.common.util.Coordinate

class SceneRenderer(
    val debugRenderer: DebugRenderer,
    val cameraManager: CameraManager,
    private val scene: Scene,
    val grid: Grid
) : VisualContextProvider {
    val interiorFadeSpeed = 20f
    var interiorFadeAlpha = 1f

    private val visualContexts = mutableMapOf<Coordinate, VisualContext>()

    fun render(spriteBatch: SpriteBatch) {
        visualContexts.clear()

        val isInsideInterior = cameraManager.isInsideInterior()
        interiorFadeAlpha = if (isInsideInterior) {
            MathUtils.lerp(interiorFadeAlpha, 0f, Gdx.graphics.deltaTime * interiorFadeSpeed)
        } else {
            MathUtils.lerp(interiorFadeAlpha, 1f, Gdx.graphics.deltaTime * interiorFadeSpeed)
        }

        scene.beginUpdate()
        for (renderable in scene.getOrderedRenderables()) {
            renderable.update(Gdx.graphics.deltaTime)

            val visualContext = visualContexts.getOrPut(renderable.coordinate) {
                VisualContext(
                    this,
                    renderable.coordinate,
                    cameraManager.focusCoordinate
                )
            }

            if (renderable.coordinate.z > cameraManager.focusCoordinate.z) {
                visualContext.interiorFadeAlpha = interiorFadeAlpha
            } else {
                visualContext.interiorFadeAlpha = 1f
            }
            renderable.render(this, spriteBatch, visualContext)
        }
        scene.endUpdate()
    }

    override fun getVisualContext(coordinate: Coordinate): VisualContext {
        return visualContexts.getOrPut(coordinate) {
            VisualContext(
                this,
                coordinate,
                cameraManager.focusCoordinate
            )
        }
    }
}
