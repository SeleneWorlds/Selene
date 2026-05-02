package com.seleneworlds.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.client.rendering.visual.Visual
import com.seleneworlds.common.script.ExposedApi

interface Visual2D : Visual, ExposedApi<Visual2DApi> {
    override val api: Visual2DApi
    fun render(batch: Batch, x: Float, y: Float)
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
}
