package world.selene.client.rendering.scene

import com.badlogic.gdx.graphics.g2d.Batch
import world.selene.client.rendering.environment.Environment
import world.selene.common.grid.Coordinate

interface Renderable {
    val coordinate: Coordinate
    val sortLayerOffset: Int
    val sortLayer: Int
    val localSortLayer: Int
    fun update(delta: Float)
    fun render(batch: Batch, environment: Environment)

    fun addedToScene(scene: Scene)
    fun removedFromScene(scene: Scene)
}