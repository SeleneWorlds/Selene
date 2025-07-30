package world.selene.client.scene

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import world.selene.client.rendering.SceneRenderer
import world.selene.client.visual.VisualContext
import world.selene.common.util.Coordinate

interface Renderable {
    val coordinate: Coordinate
    val sortLayerOffset: Int
    val sortLayer: Int
    val localSortLayer: Int
    fun update(delta: Float)
    fun render(sceneRenderer: SceneRenderer, spriteBatch: SpriteBatch, visualContext: VisualContext)
}