package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion

class TextureRegionDrawable(val textureRegion: TextureRegion, val offsetX: Float, val offsetY: Float) : Drawable {
    override fun update(delta: Float) = Unit
    override fun render(batch: Batch, x: Float, y: Float) {
        batch.draw(textureRegion, x + offsetX, y + offsetY)
    }
}
