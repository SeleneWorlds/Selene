package com.seleneworlds.client.rendering

import com.seleneworlds.common.util.Disposable
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.client.timeline.TimelinePlayer
import com.seleneworlds.client.tiles.Tile

class SceneRenderer(
    private val scene: Scene,
    private val environment: Environment,
    private val timelinePlayer: TimelinePlayer
) : Disposable {

    private val tileGrid = TileGridRenderer(environment)

    override fun dispose() {
        tileGrid.dispose()
    }

    fun setTileGridVisible(visible: Boolean) {
        tileGrid.visible = visible
    }

    fun render(batch: Batch) {
        environment.update(Gdx.graphics.deltaTime)
        timelinePlayer.update(Gdx.graphics.deltaTime)

        scene.beginUpdate()
        val renderables = scene.getOrderedRenderables()
        if (tileGrid.visible) {
            // Ground transitions share a coordinate with the base tile, but
            // have higher local sort layers. Keep the grid above that terrain.
            val groundOrders = renderables.filterIsInstance<Tile>()
                .filter { !it.visual.occlusionFade && it.visual.surfaceHeight == 0f }
                .groupBy { it.coordinate }
                .mapValues { (_, tiles) -> tiles.minOf { it.sortLayer }.let { layer ->
                    layer to tiles.filter { it.sortLayer == layer }.maxOf { it.localSortLayer }.toFloat()
                } }
            val entries = renderables.map { renderable ->
                val ground = groundOrders[renderable.coordinate]
                val sortLayer = if (renderable !is Tile && ground != null)
                    minOf(renderable.sortLayer, ground.first) else renderable.sortLayer
                val localOrder = if (renderable !is Tile)
                    maxOf(renderable.localSortLayer.toFloat(),
                        if (ground?.first == sortLayer) ground.second else 0f) + 0.5f
                    else renderable.localSortLayer.toFloat()
                Triple(sortLayer, localOrder, {
                    renderable.update(Gdx.graphics.deltaTime)
                    renderable.render(batch, environment)
                })
            } + tileGrid.coordinates().map { coordinate ->
                val ground = groundOrders[coordinate]
                val baseLayer = environment.grid.getSortLayer(coordinate, 0)
                val sortLayer = minOf(baseLayer, ground?.first ?: baseLayer)
                val localOrder = if (ground?.first == sortLayer) ground.second + 0.25f else 0.25f
                Triple(sortLayer, localOrder, {
                    tileGrid.render(batch, coordinate)
                })
            }
            entries.sortedWith(compareByDescending<Triple<Int, Float, () -> Unit>> { it.first }
                .thenBy { it.second }).forEach { it.third() }
        } else {
            for (renderable in renderables) {
                renderable.update(Gdx.graphics.deltaTime)
                renderable.render(batch, environment)
            }
        }
        scene.endUpdate()
    }

}
