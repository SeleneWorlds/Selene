package com.seleneworlds.client.rendering.visual2d

import com.seleneworlds.client.rendering.visual.VisualDefinition

class DrawableVisual2DApi(override val delegate: DrawableVisual2D) : Visual2DApi {

    fun getDrawable() = delegate.drawable.api

    fun getDefinition(): VisualDefinition {
        return delegate.visualDefinition
    }

    override fun getMetadata(key: String): Any? {
        return delegate.metadata[key]
    }
}
