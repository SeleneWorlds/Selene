package com.seleneworlds.client.rendering.scene

import com.badlogic.gdx.graphics.g2d.Batch
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.common.grid.Coordinate

interface Renderable {
    val coordinate: Coordinate
    val sortLayerOffset: Int
    val sortLayer: Int
    val localSortLayer: Int
    fun update(delta: Float)
    fun render(batch: Batch, environment: Environment)

    fun addedToScene(scene: Scene)
    fun removedFromScene(scene: Scene)
}