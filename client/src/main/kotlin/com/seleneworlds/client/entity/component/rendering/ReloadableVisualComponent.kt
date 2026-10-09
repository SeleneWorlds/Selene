package com.seleneworlds.client.entity.component.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Matrix4
import com.seleneworlds.client.entity.component.EntityComponent
import com.seleneworlds.client.entity.component.TickableComponent
import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.rendering.visual.ReloadableVisual
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.util.Disposable

class ReloadableVisualComponent(val visual: ReloadableVisual, override val positioner: ComponentPositioner) : EntityComponent,
    TickableComponent, RenderableComponent, Disposable, IsoComponent, ExposedApi<ReloadableVisualComponentApi> {
    override val api = ReloadableVisualComponentApi(this)
    var red = 1f
    var green = 1f
    var blue = 1f
    var alpha = 1f
    var scale = 1f
        set(value) {
            require(value.isFinite() && value > 0f) { "Scale must be finite and positive" }
            field = value
        }
    private val savedTransform = Matrix4()
    private val scaledTransform = Matrix4()

    override val sortLayerOffset: Int
        get() = visual.sortLayerOffset

    override val surfaceHeight: Float
        get() = visual.surfaceHeight * scale

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        visual.getBounds(x, y, outRect)
        return outRect.set(
            x + (outRect.x - x) * scale, y + (outRect.y - y) * scale,
            outRect.width * scale, outRect.height * scale
        )
    }

    override fun update(entity: Entity, delta: Float) {
        visual.update(delta)
    }

    override fun dispose() {
        visual.dispose()
    }

    override fun render(
        entity: Entity,
        batch: Batch,
        x: Float,
        y: Float
    ) {
        if (red != 1f || green != 1f || blue != 1f || alpha != 1f) {
            batch.setColor(red, green, blue, alpha)
        }
        if (scale == 1f) {
            visual.render(batch, x, y)
        } else {
            savedTransform.set(batch.transformMatrix)
            scaledTransform.set(savedTransform).translate(x, y, 0f).scale(scale, scale, 1f).translate(-x, -y, 0f)
            batch.transformMatrix = scaledTransform
            try {
                visual.render(batch, x, y)
            } finally {
                batch.transformMatrix = savedTransform
            }
        }
        batch.color = Color.WHITE
    }

    override fun toString(): String {
        return "ReloadableVisualComponent(visual=$visual)"
    }
}
