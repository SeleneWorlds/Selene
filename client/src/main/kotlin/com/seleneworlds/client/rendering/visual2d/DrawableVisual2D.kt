package com.seleneworlds.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.rendering.drawable.Drawable
import com.seleneworlds.client.rendering.visual.VisualDefinition
import com.seleneworlds.common.threading.Awaitable
import com.seleneworlds.common.data.MetadataHolder

class DrawableVisual2D(
    val visualDefinition: VisualDefinition,
    override val drawable: Drawable
) : Visual2D, DrawableVisual, MetadataHolder by visualDefinition {
    override val api = DrawableVisual2DApi(this)
    var shouldUpdate = true

    override fun initialize(): Awaitable<Void?> {
        return drawable.initialize()
    }

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
        return "DrawableVisual2D(visual=${visualDefinition.identifier})"
    }

}
