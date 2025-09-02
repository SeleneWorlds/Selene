package world.selene.client.rendering

import com.badlogic.gdx.graphics.g2d.Batch

interface Drawable {
    fun update(delta: Float)
    fun render(batch: Batch, x: Float, y: Float)
}