package com.seleneworlds.client.entity.component.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.entity.component.EntityComponent
import com.seleneworlds.client.entity.component.TickableComponent
import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.rendering.visual2d.Visual2D
import com.seleneworlds.common.script.ExposedApi

class Visual2DComponent(val visual: Visual2D, override val positioner: ComponentPositioner) : EntityComponent,
    TickableComponent, RenderableComponent, ExposedApi<Visual2DComponentApi> {
    override val api = Visual2DComponentApi(this)
    var red = 1f
    var green = 1f
    var blue = 1f
    var alpha = 1f

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        return visual.getBounds(x, y, outRect)
    }

    override fun update(entity: Entity, delta: Float) {
        visual.update(delta)
    }

    override fun render(
        entity: Entity,
        batch: Batch,
        x: Float,
        y: Float
    ) {
        if (red != 1f || green != 1f || blue != 1f || alpha != 1f) {
            batch.color = batch.color.mul(red, green, blue, alpha)
        }
        visual.render(batch, x, y)
    }

    override fun toString(): String {
        return "Visual2DComponent(visual=${visual})"
    }
}
