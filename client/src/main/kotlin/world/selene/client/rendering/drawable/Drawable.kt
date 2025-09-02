package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle

interface Drawable {
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    fun update(delta: Float)
    fun render(batch: Batch, x: Float, y: Float)
}