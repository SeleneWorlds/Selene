package world.selene.client.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import world.selene.client.maps.Entity

interface RenderableComponent {
    fun render(entity: Entity, batch: Batch, x: Float, y: Float)
}