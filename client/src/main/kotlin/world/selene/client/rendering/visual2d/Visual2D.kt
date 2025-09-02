package world.selene.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import world.selene.client.rendering.visual.Visual

interface Visual2D : Visual {
    fun render(batch: Batch, x: Float, y: Float)
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
}