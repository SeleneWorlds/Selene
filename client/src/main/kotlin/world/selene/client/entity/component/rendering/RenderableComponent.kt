package world.selene.client.entity.component.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import world.selene.client.entity.Entity

interface RenderableComponent {
    val positioner: ComponentPositioner
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    fun render(entity: Entity, batch: Batch, x: Float, y: Float)
}