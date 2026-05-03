package com.seleneworlds.client.rendering.visual2d.iso

import com.seleneworlds.client.rendering.visual.IsoVisualApi

class DynamicDrawableIsoVisualApi(override val delegate: DynamicDrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = delegate.drawable.api

    override fun getSurfaceHeight(): Float {
        return delegate.surfaceHeight
    }

    override fun getMetadata(key: String): Any? {
        return delegate.metadata[key]
    }
}
