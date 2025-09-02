package world.selene.client.entity.component

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import world.selene.client.maps.Entity

interface RenderableComponent {
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    fun render(entity: Entity, batch: Batch, x: Float, y: Float)
}