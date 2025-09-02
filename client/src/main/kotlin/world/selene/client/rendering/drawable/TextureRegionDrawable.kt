package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle

class TextureRegionDrawable(val textureRegion: TextureRegion, val offsetX: Float, val offsetY: Float) : Drawable {
    override fun update(delta: Float) = Unit
    override fun render(batch: Batch, x: Float, y: Float) {
        batch.draw(textureRegion, x + offsetX, y + offsetY)
    }

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        outRect.set(
            x + offsetX,
            y + offsetY,
            textureRegion.regionWidth.toFloat(),
            textureRegion.regionHeight.toFloat()
        )
        return outRect
    }
}
