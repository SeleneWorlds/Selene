package com.seleneworlds.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle

class TextureRegionDrawable(
    val textureRegion: TextureRegion,
    val offsetX: Float,
    val offsetY: Float,
    private val centerOnRegionWidth: Boolean = false
) : Drawable {
    override val api = TextureRegionDrawableApi(this)

    private val resolvedOffsetX: Float
        get() = if (centerOnRegionWidth) offsetX - textureRegion.regionWidth / 2f else offsetX

    override fun update(delta: Float) {
        (textureRegion as? ReloadableTextureRegion)?.update()
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        batch.draw(textureRegion, x + resolvedOffsetX, y + offsetY)
    }

    override fun render(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        batch.draw(textureRegion, x + resolvedOffsetX, y + offsetY, width, height)
    }

    override fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    ) {
        batch.draw(textureRegion, x + resolvedOffsetX, y + offsetY, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        outRect.set(
            x + resolvedOffsetX,
            y + offsetY,
            textureRegion.regionWidth.toFloat(),
            textureRegion.regionHeight.toFloat()
        )
        return outRect
    }

    fun withoutOffset(): TextureRegionDrawable {
        return TextureRegionDrawable(textureRegion, 0f, 0f)
    }

    override fun toString(): String {
        return "TextureRegionDrawable(texture=${textureRegion.texture}, offsetX=$offsetX, offsetY=$offsetY, centerOnRegionWidth=$centerOnRegionWidth)"
    }
}
