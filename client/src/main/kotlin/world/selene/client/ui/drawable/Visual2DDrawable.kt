package world.selene.client.ui.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import world.selene.client.rendering.visual2d.Visual2D

class Visual2DDrawable(private val visual: Visual2D) : BaseDrawable(), TransformDrawable {

    init {
        val rectangle = visual.getBounds(0f, 0f, Rectangle())
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
        visual.render(batch, x, y)
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
        visual.render(batch, x, y)
    }

}