package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import world.selene.client.rendering.drawable.Drawable

class DynamicDrawableIsoVisual(
    private val drawableProvider: () -> Drawable?,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {
    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawableProvider()?.update(delta)
        }
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawableProvider()?.render(batch, x, y)
    }
}

