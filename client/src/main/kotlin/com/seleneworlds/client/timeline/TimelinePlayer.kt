package com.seleneworlds.client.timeline

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.Logger

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
        val instanceId: String,
        val timeline: String,
        var elapsed: Float,
        var nextEvent: Int,
        val events: List<TimelineEvent>,
        val parameters: JsonObject
    )

    private data class OwnedEffect(
        val instanceId: String,
        val timeline: String,
        val effect: ParticleEffect,
        val screenSpace: Boolean,
    )

    private val playbacks = mutableListOf<Playback>()
    private val effects = mutableListOf<OwnedEffect>()
    private val instanceTimelines = mutableMapOf<String, String>()
    private val pendingEffects = mutableMapOf<String, Int>()
    private val stoppedInstances = mutableSetOf<String>()

    fun play(instanceId: String, identifier: String, parameters: JsonObject) {
        val definition = timelines.get(Identifier.parse(identifier))
        if (definition == null) {
            logger.warn("Unknown timeline $identifier")
            return
        }
        stoppedInstances.remove(instanceId)
        instanceTimelines[instanceId] = identifier
        playbacks += Playback(instanceId, identifier, 0f, 0, definition.events.sortedBy { it.time }, parameters)
        update(0f)
    }

    fun stop(instanceId: String?, timeline: String?) {
        val targets = if (instanceId != null) listOf(instanceId)
        else instanceTimelines.filterValues { it == timeline }.keys.toList()
        for (target in targets) {
            stoppedInstances += target
            playbacks.removeAll { it.instanceId == target }
            effects.filter { it.instanceId == target }.forEach { it.effect.allowCompletion() }
            cleanupInstance(target)
        }
    }

    fun update(delta: Float) {
        val playbackIterator = playbacks.iterator()
        while (playbackIterator.hasNext()) {
            val playback = playbackIterator.next()
            playback.elapsed += delta
            while (playback.nextEvent < playback.events.size &&
                playback.events[playback.nextEvent].time <= playback.elapsed) {
                execute(playback.events[playback.nextEvent++], playback)
            }
            if (playback.nextEvent == playback.events.size) {
                playbackIterator.remove()
                cleanupInstance(playback.instanceId)
            }
        }

        val effectIterator = effects.iterator()
        while (effectIterator.hasNext()) {
            val owned = effectIterator.next()
            if (owned.screenSpace) {
                configureScreenEffect(owned.effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
                owned.effect.update(delta)
            }
            if (owned.effect.isComplete) {
                effectIterator.remove()
                cleanupInstance(owned.instanceId)
            }
        }
    }

    fun renderScreen(batch: Batch, width: Float, height: Float) {
        for (owned in effects) {
            if (!owned.screenSpace) continue
            configureScreenEffect(owned.effect, width, height)
            owned.effect.draw(batch)
        }
    }

    private fun execute(event: TimelineEvent, playback: Playback) {
        when (event) {
            is ParticleSystemTimelineEvent -> playParticleSystem(event, playback)
            is VisualAnimationTimelineEvent -> logger.warn("Visual animation timeline events are not supported by this client")
        }
    }

    private fun playParticleSystem(event: ParticleSystemTimelineEvent, playback: Playback) {
        markPending(playback.instanceId, 1)
        val coordinate = if (event.space == ParticleSystemSpace.WORLD) {
            playback.parameters[event.position]?.toCoordinate()
        } else null
        if (event.space == ParticleSystemSpace.WORLD && coordinate == null) {
            logger.warn("Particle event ${event.particle} requires coordinate parameter ${event.position}")
            markPending(playback.instanceId, -1)
            return
        }
        val definition = particles.get(Identifier.parse(event.particle))
        if (definition == null) {
            logger.warn("Unknown particle system ${event.particle}")
            markPending(playback.instanceId, -1)
            return
        }
        assets.loadTextureAsync(definition.texture).invokeOnCompletion { error ->
            if (error != null) {
                mainThread.runOnMainThread { markPending(playback.instanceId, -1) }
                return@invokeOnCompletion
            }
            mainThread.runOnMainThread {
                if (stoppedInstances.contains(playback.instanceId)) {
                    markPending(playback.instanceId, -1)
                    return@runOnMainThread
                }
                val texture = assets.getLoadedTexture(definition.texture)
                if (texture == null) {
                    markPending(playback.instanceId, -1)
                    return@runOnMainThread
                }
                val effect = particleLoader.load(definition, texture)
                if (event.space == ParticleSystemSpace.SCREEN) {
                    configureScreenEffect(effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
                    effect.start()
                } else {
                    scene.add(ParticleEffectRenderable(coordinate!!, effect, grid))
                }
                effects += OwnedEffect(playback.instanceId, playback.timeline, effect, event.space == ParticleSystemSpace.SCREEN)
                markPending(playback.instanceId, -1)
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

    private fun markPending(instanceId: String, delta: Int) {
        val count = (pendingEffects[instanceId] ?: 0) + delta
        if (count > 0) pendingEffects[instanceId] = count else pendingEffects.remove(instanceId)
        cleanupInstance(instanceId)
    }

    private fun cleanupInstance(instanceId: String) {
        if (playbacks.any { it.instanceId == instanceId }) return
        if (effects.any { it.instanceId == instanceId }) return
        if (pendingEffects.containsKey(instanceId)) return
        instanceTimelines.remove(instanceId)
        stoppedInstances.remove(instanceId)
    }
}

private fun JsonElement.toCoordinate(): Coordinate? {
    val value = this as? JsonObject ?: return null
    val x = value["x"]?.jsonPrimitive?.intOrNull ?: return null
    val y = value["y"]?.jsonPrimitive?.intOrNull ?: return null
    val z = value["z"]?.jsonPrimitive?.intOrNull ?: return null
    return Coordinate(x, y, z)
}
