package com.seleneworlds.client.rendering.visual2d.iso

import com.seleneworlds.client.rendering.visual.IsoVisualApi

class DynamicDrawableIsoVisualApi(val visual: DynamicDrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = visual.drawable.api

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }
}
