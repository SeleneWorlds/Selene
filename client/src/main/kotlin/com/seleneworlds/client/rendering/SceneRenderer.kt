package com.seleneworlds.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.scene.Scene

class SceneRenderer(private val scene: Scene, private val environment: Environment) {

    fun render(batch: Batch) {
        environment.update(Gdx.graphics.deltaTime)

        scene.beginUpdate()
        for (renderable in scene.getOrderedRenderables()) {
            renderable.update(Gdx.graphics.deltaTime)
            renderable.render(batch, environment)
        }
        scene.endUpdate()
    }

}
