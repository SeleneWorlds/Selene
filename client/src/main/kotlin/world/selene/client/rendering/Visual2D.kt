package world.selene.client.rendering

import com.badlogic.gdx.graphics.g2d.Batch

interface Visual2D : Visual {
    fun render(batch: Batch, x: Float, y: Float)
}