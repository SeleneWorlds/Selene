package world.selene.client.rendering.visual2d.iso

import world.selene.client.rendering.visual.IsoVisualApi
import world.selene.client.rendering.visual2d.Visual2D

interface IsoVisual : Visual2D {
    override val api: IsoVisualApi
    val sortLayerOffset: Int
    val surfaceHeight: Float
}
