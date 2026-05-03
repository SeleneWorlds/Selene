package com.seleneworlds.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.rendering.visual.AnimatorVisualDefinition
import com.seleneworlds.client.rendering.drawable.Drawable
import com.seleneworlds.client.rendering.visual2d.DrawableVisual
import com.seleneworlds.common.threading.Awaitable
import com.seleneworlds.common.data.MetadataHolder

class DynamicDrawableIsoVisual(
    private val visualDef: AnimatorVisualDefinition,
    private val drawableProvider: () -> Drawable?,
    private val initializeAwaitable: Awaitable<Void?> = Awaitable.completed(null),
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual, DrawableVisual, MetadataHolder by visualDef {
    override val api = DynamicDrawableIsoVisualApi(this)

    override val drawable: Drawable get() = drawableProvider() ?: Drawable.Empty

    var shouldUpdate = true

    override fun initialize(): Awaitable<Void?> {
        return initializeAwaitable
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
        return "DynamicDrawableIsoVisual(visual=${visualDef.identifier})"
    }
}
