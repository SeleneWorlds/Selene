package world.selene.client.rendering.visual2d.iso

import world.selene.client.rendering.visual.IsoVisualApi
import world.selene.client.rendering.visual.VisualDefinition

class DrawableIsoVisualApi(val visual: DrawableIsoVisual) : IsoVisualApi {

    fun getDrawable() = visual.drawable.api

    fun getDefinition(): VisualDefinition {
        return visual.visualDefinition
    }

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }
}
