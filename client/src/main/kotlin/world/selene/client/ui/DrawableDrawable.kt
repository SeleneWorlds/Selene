package world.selene.client.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import world.selene.client.rendering.drawable.Drawable

class DrawableDrawable(private val drawable: Drawable) : BaseDrawable(), TransformDrawable {

    init {
        val rectangle = drawable.getBounds(0f, 0f, Rectangle())
        minWidth = rectangle.width
        minHeight = rectangle.height
    }

    override fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        drawable.render(batch, x, y, width, height)
    }

    override fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Float
    ) {
        drawable.render(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

}