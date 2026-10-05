package com.seleneworlds.client.particles

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.scene.Renderable
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.common.grid.Coordinate

class ParticleEffectRenderable(
    override val coordinate: Coordinate,
    private val effect: ParticleEffect,
    private val grid: ClientGrid
) : Renderable {
    override val sortLayerOffset = 0
    override val sortLayer get() = grid.getSortLayer(coordinate, sortLayerOffset)
    override val localSortLayer = 1
    private var scene: Scene? = null

    init {
        effect.setPosition(grid.getScreenX(coordinate), grid.getScreenY(coordinate))
        effect.start()
    }

    override fun update(delta: Float) {
        effect.update(delta)
        if (effect.isComplete) scene?.remove(this)
    }

    override fun render(batch: Batch, environment: Environment) {
        val oldColor = batch.color.cpy()
        batch.setColor(1f, 1f, 1f, 1f)
        effect.draw(batch)
        batch.color = oldColor
    }

    override fun addedToScene(scene: Scene) { this.scene = scene }

    override fun removedFromScene(scene: Scene) {
        this.scene = null
        // The texture belongs to AssetStorage; ParticleEffect.dispose() would dispose that shared texture.
    }
}
