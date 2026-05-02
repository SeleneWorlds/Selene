package world.selene.client.rendering.visual

import world.selene.client.rendering.visual2d.DrawableVisual

class ReloadableVisualApi(val visual: ReloadableVisual) : IsoVisualApi {

    fun getDrawable() = (visual.visual as? DrawableVisual)?.drawable?.api

    override fun getSurfaceHeight(): Float {
        return visual.surfaceHeight
    }
}
