package com.seleneworlds.client.timeline

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
import org.slf4j.Logger
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.camera.CameraManager
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.particles.ParticleEffectRenderable
import com.seleneworlds.client.particles.ParticleSystemLoader
import com.seleneworlds.client.particles.ParticleSystemRegistry
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.threading.MainThreadDispatcher

class TimelinePlayer(
    private val timelines: TimelineRegistry,
    private val particles: ParticleSystemRegistry,
    private val particleLoader: ParticleSystemLoader,
    private val assets: AssetProvider,
    private val mainThread: MainThreadDispatcher,
    private val scene: Scene,
    private val grid: ClientGrid,
    private val cameraManager: CameraManager,
    private val logger: Logger
) {
    private data class Playback(
        var elapsed: Float,
        var nextEvent: Int,
        val events: List<TimelineEvent>,
        val parameters: JsonObject
    )

    private data class ScreenEffect(val effect: ParticleEffect)

    private val playbacks = mutableListOf<Playback>()
    private val screenEffects = mutableListOf<ScreenEffect>()

    fun play(identifier: String, parameters: JsonObject) {
        val definition = timelines.get(Identifier.parse(identifier))
        if (definition == null) {
            logger.warn("Unknown timeline $identifier")
            return
        }
        playbacks += Playback(0f, 0, definition.events.sortedBy { it.time }, parameters)
        update(0f)
    }

    fun update(delta: Float) {
        val iterator = playbacks.iterator()
        while (iterator.hasNext()) {
            val playback = iterator.next()
            playback.elapsed += delta
            while (playback.nextEvent < playback.events.size &&
                playback.events[playback.nextEvent].time <= playback.elapsed) {
                execute(playback.events[playback.nextEvent++], playback.parameters)
            }
            if (playback.nextEvent == playback.events.size) iterator.remove()
        }
        val screenIterator = screenEffects.iterator()
        while (screenIterator.hasNext()) {
            val effect = screenIterator.next().effect
            configureScreenEffect(effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
            effect.update(delta)
            if (effect.isComplete) screenIterator.remove()
        }
    }

    fun renderScreen(batch: Batch, width: Float, height: Float) {
        for ((effect) in screenEffects) {
            configureScreenEffect(effect, width, height)
            effect.draw(batch)
        }
    }

    private fun execute(event: TimelineEvent, parameters: JsonObject) {
        when (event) {
            is ParticleSystemTimelineEvent -> playParticleSystem(event, parameters)
            is VisualAnimationTimelineEvent -> logger.warn("Visual animation timeline events are not supported by this client")
        }
    }

    private fun playParticleSystem(event: ParticleSystemTimelineEvent, parameters: JsonObject) {
        val coordinate = if (event.space == ParticleSystemSpace.WORLD) parameters[event.position]?.toCoordinate() else null
        if (event.space == ParticleSystemSpace.WORLD && coordinate == null) {
            logger.warn("Particle event ${event.particle} requires coordinate parameter ${event.position}")
            return
        }
        val definition = particles.get(Identifier.parse(event.particle))
        if (definition == null) {
            logger.warn("Unknown particle system ${event.particle}")
            return
        }
        assets.loadTextureAsync(definition.texture).invokeOnCompletion { error ->
            if (error != null) return@invokeOnCompletion
            mainThread.runOnMainThread {
                val texture = assets.getLoadedTexture(definition.texture) ?: return@runOnMainThread
                val effect = particleLoader.load(definition, texture)
                if (event.space == ParticleSystemSpace.SCREEN) {
                    configureScreenEffect(effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
                    effect.start()
                    screenEffects += ScreenEffect(effect)
                } else {
                    scene.add(ParticleEffectRenderable(coordinate!!, effect, grid))
                }
            }
        }
    }

    private fun configureScreenEffect(effect: ParticleEffect, width: Float, height: Float) {
        val emitter = effect.emitters.first()
        emitter.spawnShape.setShape(ParticleEmitter.SpawnShape.square)
        emitter.spawnWidth.setLow(width)
        emitter.spawnWidth.setHigh(width)
        emitter.spawnHeight.setLow(0f)
        emitter.spawnHeight.setHigh(0f)
        effect.setPosition(width / 2f, height)
    }
}

private fun JsonElement.toCoordinate(): Coordinate? {
    val value = this as? JsonObject ?: return null
    val x = value["x"]?.jsonPrimitive?.intOrNull ?: return null
    val y = value["y"]?.jsonPrimitive?.intOrNull ?: return null
    val z = value["z"]?.jsonPrimitive?.intOrNull ?: return null
    return Coordinate(x, y, z)
}
