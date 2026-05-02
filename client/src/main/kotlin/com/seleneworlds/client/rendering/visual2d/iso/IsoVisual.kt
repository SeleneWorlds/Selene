package com.seleneworlds.client.rendering.visual2d.iso

import com.seleneworlds.client.rendering.visual.IsoVisualApi
import com.seleneworlds.client.rendering.visual2d.Visual2D

interface IsoVisual : Visual2D {
    override val api: IsoVisualApi
    val sortLayerOffset: Int
    val surfaceHeight: Float
}
