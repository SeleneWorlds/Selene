package world.selene.client.rendering.visual2d

import world.selene.client.rendering.visual.VisualDefinition

class DrawableVisual2DApi(val visual: DrawableVisual2D) : Visual2DApi {

    fun getDrawable() = visual.drawable.api

    fun getDefinition(): VisualDefinition {
        return visual.visualDefinition
    }
}
