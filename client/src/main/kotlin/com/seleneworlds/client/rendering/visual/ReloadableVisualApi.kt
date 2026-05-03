package com.seleneworlds.client.rendering.visual

import com.seleneworlds.client.rendering.visual2d.DrawableVisual
import com.seleneworlds.common.data.MetadataHolder

class ReloadableVisualApi(override val delegate: ReloadableVisual) : IsoVisualApi {

    fun getDrawable() = (delegate.visual as? DrawableVisual)?.drawable?.api

    override fun getSurfaceHeight(): Float {
        return delegate.surfaceHeight
    }

    override fun getMetadata(key: String): Any? {
        return (delegate.visual as? MetadataHolder)?.metadata[key]
    }
}
