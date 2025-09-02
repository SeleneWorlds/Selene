package world.selene.client.visual

import com.badlogic.gdx.graphics.g2d.Batch
import world.selene.client.rendering.Drawable
import world.selene.client.rendering.IsoVisual

class DrawableIsoVisual(
    private val drawable: Drawable,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {
    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable.update(delta)
        }
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable.render(batch, x, y)
    }
}

