package com.seleneworlds.client.timeline

import com.badlogic.gdx.graphics.g2d.Batch
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.scene.Renderable
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.client.rendering.visual2d.iso.IsoVisual
import com.seleneworlds.common.grid.Coordinate

internal class TimelineVisualRenderable(
    override val coordinate: Coordinate,
    private val visual: IsoVisual,
    private val event: VisualAnimationTimelineEvent,
    private val duration: Float,
    private val grid: ClientGrid
) : Renderable {
    override val sortLayerOffset get() = visual.sortLayerOffset
    override val sortLayer get() = grid.getSortLayer(coordinate, sortLayerOffset)
    override val localSortLayer = 1
    var complete = false
        private set
    private var elapsed = 0f

    override fun update(delta: Float) {
        elapsed += delta
        visual.update(delta)
        complete = elapsed >= duration
    }

    override fun render(batch: Batch, environment: Environment) {
        val oldColor = batch.color.cpy()
        batch.color.set(environment.getColor(coordinate))
        batch.color.a *= event.keyedFloat("alpha", 1f, elapsed).coerceIn(0f, 1f)
        visual.render(
            batch,
            grid.getScreenX(coordinate),
            grid.getScreenY(coordinate) + if (event.ignoresElevation) 0f else environment.getSurfaceOffset(coordinate)
        )
        batch.color = oldColor
    }

    override fun addedToScene(scene: Scene) = Unit
    override fun removedFromScene(scene: Scene) = Unit
}
