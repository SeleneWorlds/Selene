package world.selene.client.rendering.visual2d.iso

import world.selene.client.rendering.visual.IsoVisualApi

class DynamicDrawableIsoVisualApi(val visual: DynamicDrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = visual.drawable.api

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }
}
