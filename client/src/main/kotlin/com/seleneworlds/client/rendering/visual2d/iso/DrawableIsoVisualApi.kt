package com.seleneworlds.client.rendering.visual2d.iso

import com.seleneworlds.client.rendering.visual.IsoVisualApi
import com.seleneworlds.client.rendering.visual.VisualDefinition

class DrawableIsoVisualApi(override val delegate: DrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = delegate.drawable.api

    fun getDefinition(): VisualDefinition {
        return delegate.visualDefinition
    }

    override fun getSurfaceHeight(): Float {
        return delegate.surfaceHeight
    }

    override fun getMetadata(key: String): Any? {
        return delegate.metadata[key]
    }
}
