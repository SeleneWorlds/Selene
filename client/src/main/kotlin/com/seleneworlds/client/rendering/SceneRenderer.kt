package com.seleneworlds.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.client.timeline.TimelinePlayer

class SceneRenderer(
    private val scene: Scene,
    private val environment: Environment,
    private val timelinePlayer: TimelinePlayer
) {

    fun render(batch: Batch) {
        environment.update(Gdx.graphics.deltaTime)
        timelinePlayer.update(Gdx.graphics.deltaTime)

        scene.beginUpdate()
        for (renderable in scene.getOrderedRenderables()) {
            renderable.update(Gdx.graphics.deltaTime)
            renderable.render(batch, environment)
        }
        scene.endUpdate()
    }

}
