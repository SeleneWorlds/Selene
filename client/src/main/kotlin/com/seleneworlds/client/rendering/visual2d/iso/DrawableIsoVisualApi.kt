package com.seleneworlds.client.rendering.visual2d.iso

import com.seleneworlds.client.rendering.visual.IsoVisualApi
import com.seleneworlds.client.rendering.visual.VisualDefinition

class DrawableIsoVisualApi(val visual: DrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = visual.drawable.api

    fun getDefinition(): VisualDefinition {
        return visual.visualDefinition
    }

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }

    override fun getMetadata(key: String): Any? {
        return visual.metadata[key]
    }
}
