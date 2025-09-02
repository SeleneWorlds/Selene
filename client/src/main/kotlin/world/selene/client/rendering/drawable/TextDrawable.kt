package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle

class TextDrawable(private val font: BitmapFont, private val glyphLayout: GlyphLayout) : Drawable {
    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        outRect.set(x, y, glyphLayout.width, glyphLayout.height)
        return outRect
    }

    override fun update(delta: Float) = Unit

    override fun render(batch: Batch, x: Float, y: Float) {
        font.draw(batch, glyphLayout, x, y)
    }

    override fun render(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        font.draw(batch, glyphLayout, x, y)
    }

    override fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    ) {
        render(batch, x, y)
    }
}