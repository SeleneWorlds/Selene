package com.seleneworlds.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.rendering.visual.VisualDefinition
import com.seleneworlds.client.rendering.drawable.Drawable
import com.seleneworlds.client.rendering.visual2d.DrawableVisual
import com.seleneworlds.common.data.MetadataHolder

class DrawableIsoVisual(
    val visualDefinition: VisualDefinition,
    override val drawable: Drawable,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual, DrawableVisual, MetadataHolder by visualDefinition {
    override val api = DrawableIsoVisualApi(this)
    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable.update(delta)
        }
    }

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return drawable.getBounds(x, y, outRect)
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable.render(batch, x, y)
    }

    override fun toString(): String {
        return "DrawableIsoVisual(visual=${visualDefinition.identifier})"
    }
}
