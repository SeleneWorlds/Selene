package com.seleneworlds.client.timeline

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
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
import com.seleneworlds.client.rendering.visual.VisualCreationContext
import com.seleneworlds.client.rendering.visual.VisualFactory
import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.client.rendering.visual.AnimatedVisualDefinition
import com.seleneworlds.client.rendering.visual2d.iso.IsoVisual
import com.seleneworlds.client.sounds.SoundManager
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.util.Disposable
import com.seleneworlds.common.timeline.TimelineParameters
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.contentOrNull
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
    private val visuals: VisualRegistry,
    private val visualFactory: VisualFactory,
    private val cameraManager: CameraManager,
    private val sounds: SoundRegistry,
    private val soundManager: SoundManager,
    private val logger: Logger
) : Disposable {
    private var overlayTexture: Texture? = null

    override fun dispose() {
        overlayTexture?.dispose()
        overlayTexture = null
    }

    private data class Playback(
        val instanceId: String,
        val timeline: String,
        var elapsed: Float,
        var nextEvent: Int,
        val events: List<TimelineEvent>,
        val parameters: TimelineParameters
    )

    private data class OwnedEffect(
        val instanceId: String,
        val timeline: String,
        val effect: ParticleEffect,
        val screenSpace: Boolean,
        val event: ParticleSystemTimelineEvent,
        val parameters: TimelineParameters,
    )

    private data class OwnedVisual(
        val instanceId: String,
        val timeline: String,
        val effect: TimelineVisualRenderable
    )

    private data class OwnedOverlay(
        val instanceId: String,
        val event: ScreenOverlayTimelineEvent,
        val parameters: TimelineParameters,
        var elapsed: Float = 0f
    )

    private val playbacks = mutableListOf<Playback>()
    private val effects = mutableListOf<OwnedEffect>()
    private val visualEffects = mutableListOf<OwnedVisual>()
    private val overlays = mutableListOf<OwnedOverlay>()
    private val instanceTimelines = mutableMapOf<String, String>()
    private val instanceParameters = mutableMapOf<String, TimelineParameters>()
    private val instanceTags = mutableMapOf<String, Set<String>>()
    private val pendingEffects = mutableMapOf<String, Int>()
    private val stoppedInstances = mutableSetOf<String>()

    fun play(
        instanceId: String,
        identifier: String,
        parameters: JsonObject,
        transition: Float = 0f,
        tags: List<String> = emptyList()
    ) {
        require(instanceId.isNotBlank()) { "instanceId must not be blank" }
        require(transition.isFinite() && transition >= 0f) { "transition must be a non-negative finite number" }
        if (instanceTimelines[instanceId] == identifier && !stoppedInstances.contains(instanceId)) {
            instanceParameters.getValue(instanceId).retarget(parameters, transition)
            instanceTags[instanceId] = tags.toSet()
            return
        }
        val definition = timelines.get(Identifier.parse(identifier))
        if (definition == null) {
            logger.warn("Unknown timeline $identifier")
            return
        }
        if (instanceTimelines.containsKey(instanceId)) stop(instanceId, null)
        val liveParameters = TimelineParameters(parameters)
        stoppedInstances.remove(instanceId)
        instanceTimelines[instanceId] = identifier
        instanceParameters[instanceId] = liveParameters
        instanceTags[instanceId] = tags.toSet()
        playbacks += Playback(instanceId, identifier, 0f, 0, definition.events.sortedBy { it.time }, liveParameters)
        update(0f)
    }

    fun stop(instanceId: String?, timeline: String?, tag: String? = null) {
        val targets = if (instanceId != null) listOf(instanceId)
        else if (timeline != null) instanceTimelines.filterValues { it == timeline }.keys.toList()
        else instanceTags.filterValues { tag in it }.keys.toList()
        for (target in targets) {
            stoppedInstances += target
            playbacks.removeAll { it.instanceId == target }
            effects.filter { it.instanceId == target }.forEach { it.effect.allowCompletion() }
            visualEffects.filter { it.instanceId == target }.forEach { scene.remove(it.effect) }
            visualEffects.removeAll { it.instanceId == target }
            overlays.removeAll { it.instanceId == target }
            cleanupInstance(target)
        }
    }

    fun update(delta: Float) {
        instanceParameters.values.forEach { it.update(delta) }
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
            if (!stoppedInstances.contains(owned.instanceId)) {
                val multiplier = owned.parameters.multiplier(owned.event.emissionRateMultiplier)
                owned.effect.emitters.forEach { it.emission.setScaling(floatArrayOf(multiplier)) }
            }
            if (owned.screenSpace && !(owned.event.outdoorsOnly && cameraManager.isInsideInterior())) {
                configureScreenEffect(owned.effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
                owned.effect.update(delta)
            }
            if (owned.effect.isComplete) {
                effectIterator.remove()
                cleanupInstance(owned.instanceId)
            }
        }

        val visualIterator = visualEffects.iterator()
        while (visualIterator.hasNext()) {
            val owned = visualIterator.next()
            if (owned.effect.complete) {
                scene.remove(owned.effect)
                visualIterator.remove()
                cleanupInstance(owned.instanceId)
            }
        }

        val overlayIterator = overlays.iterator()
        while (overlayIterator.hasNext()) {
            val owned = overlayIterator.next()
            if (!(owned.event.outdoorsOnly && cameraManager.isInsideInterior())) owned.elapsed += delta
            if (owned.event.duration?.let { owned.elapsed >= it } == true) {
                overlayIterator.remove()
                cleanupInstance(owned.instanceId)
            }
        }
    }

    fun renderScreen(batch: Batch, width: Float, height: Float) {
        for (owned in effects) {
            if (!owned.screenSpace || (owned.event.outdoorsOnly && cameraManager.isInsideInterior())) continue
            configureScreenEffect(owned.effect, width, height)
            owned.effect.draw(batch)
        }
        for (owned in overlays) {
            if (owned.event.outdoorsOnly && cameraManager.isInsideInterior()) continue
            val color = Color.valueOf(owned.event.keyedString("color", owned.event.color, owned.elapsed).removePrefix("#"))
            val alpha = owned.event.keyedFloat("alpha", owned.event.alpha, owned.elapsed).coerceIn(0f, 1f) * color.a * owned.parameters.multiplier(owned.event.alphaMultiplier).coerceAtMost(1f)
            if (alpha <= 0f) continue
            batch.setColor(color.r, color.g, color.b, alpha)
            val texture = owned.event.texture?.let { assets.getLoadedTexture(it) }
                ?: if (owned.event.texture == null) whiteTexture() else continue
            batch.draw(texture, 0f, 0f, width, height)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun execute(event: TimelineEvent, playback: Playback) {
        when (event) {
            is ParticleSystemTimelineEvent -> playParticleSystem(event, playback)
            is ScreenOverlayTimelineEvent -> playScreenOverlay(event, playback)
            is SoundTimelineEvent -> {
                val sound = sounds.get(Identifier.parse(event.sound))
                if (sound == null) logger.warn("Unknown sound ${event.sound}")
                else soundManager.playSound(sound, event.volume, event.pitch)
            }
            is VisualAnimationTimelineEvent -> playVisualAnimation(event, playback)
        }
    }

    private fun playScreenOverlay(event: ScreenOverlayTimelineEvent, playback: Playback) {
        if (event.texture == null) {
            overlays += OwnedOverlay(playback.instanceId, event, playback.parameters)
            return
        }
        markPending(playback.instanceId, 1)
        assets.loadTextureAsync(event.texture).invokeOnCompletion { error ->
            mainThread.runOnMainThread {
                if (error == null && isActive(playback)) {
                    overlays += OwnedOverlay(playback.instanceId, event, playback.parameters)
                }
                markPending(playback.instanceId, -1)
            }
        }
    }

    private fun playVisualAnimation(event: VisualAnimationTimelineEvent, playback: Playback) {
        val coordinate = playback.parameters[event.position]?.toCoordinate()
        if (coordinate == null) {
            logger.warn("Visual event ${event.visual} requires coordinate parameter ${event.position}")
            return
        }
        val definition = visuals.get(Identifier.parse(event.visual))
        val visual = definition?.let { visualFactory.createVisual(it, VisualCreationContext(coordinate)) } as? IsoVisual
        if (visual == null) {
            logger.warn("Visual event references unsupported visual ${event.visual}")
            return
        }
        val duration = event.duration ?: (definition as? AnimatedVisualDefinition)?.duration ?: 1f
        visual.initialize()
        val effect = TimelineVisualRenderable(coordinate, visual, event, duration, grid)
        visualEffects += OwnedVisual(playback.instanceId, playback.timeline, effect)
        scene.add(effect)
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
                if (!isActive(playback)) {
                    markPending(playback.instanceId, -1)
                    return@runOnMainThread
                }
                val texture = assets.getLoadedTexture(definition.texture)
                if (texture == null) {
                    markPending(playback.instanceId, -1)
                    return@runOnMainThread
                }
                val effect = particleLoader.load(definition, texture)
                // Keep the emission range fixed; its scaling curve reads the live multiplier.
                effect.emitters.forEach {
                    it.emission.setLow(0f)
                    it.emission.setScaling(floatArrayOf(playback.parameters.multiplier(event.emissionRateMultiplier)))
                }
                if (event.space == ParticleSystemSpace.SCREEN) {
                    configureScreenEffect(effect, cameraManager.camera.viewportWidth, cameraManager.camera.viewportHeight)
                    effect.start()
                } else {
                    scene.add(ParticleEffectRenderable(coordinate!!, effect, grid) {
                        !event.outdoorsOnly || !cameraManager.isInsideInterior()
                    })
                }
                effects += OwnedEffect(playback.instanceId, playback.timeline, effect, event.space == ParticleSystemSpace.SCREEN, event, playback.parameters)
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

    private fun isActive(playback: Playback): Boolean =
        !stoppedInstances.contains(playback.instanceId) && instanceParameters[playback.instanceId] === playback.parameters

    private fun markPending(instanceId: String, delta: Int) {
        val count = (pendingEffects[instanceId] ?: 0) + delta
        if (count > 0) pendingEffects[instanceId] = count else pendingEffects.remove(instanceId)
        cleanupInstance(instanceId)
    }

    private fun cleanupInstance(instanceId: String) {
        if (playbacks.any { it.instanceId == instanceId }) return
        if (effects.any { it.instanceId == instanceId }) return
        if (visualEffects.any { it.instanceId == instanceId }) return
        if (overlays.any { it.instanceId == instanceId }) return
        if (pendingEffects.containsKey(instanceId)) return
        instanceTimelines.remove(instanceId)
        instanceParameters.remove(instanceId)
        instanceTags.remove(instanceId)
        stoppedInstances.remove(instanceId)
    }

    private fun whiteTexture(): Texture = overlayTexture ?: run {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        Texture(pixmap).also {
            pixmap.dispose()
            overlayTexture = it
        }
    }
}

internal fun TimelineEvent.keyedFloat(property: String, base: Float, elapsed: Float): Float =
    keyedValue(property, elapsed, JsonPrimitive(base)).floatOrNull ?: base

internal fun TimelineEvent.keyedString(property: String, base: String, elapsed: Float): String =
    keyedValue(property, elapsed, JsonPrimitive(base)).contentOrNull ?: base

private fun TimelineEvent.keyedValue(property: String, elapsed: Float, base: JsonPrimitive): JsonPrimitive {
    val propertyKeys = keys[property]?.sortedBy { it.time }.orEmpty()
    val currentIndex = propertyKeys.indexOfLast { it.time <= elapsed }
    if (currentIndex < 0) return base
    val current = propertyKeys[currentIndex]
    val next = propertyKeys.getOrNull(currentIndex + 1) ?: return current.value
    if (current.interpolation == TimelineKeyInterpolation.STEP) return current.value

    val from = current.value.floatOrNull ?: return current.value
    val to = next.value.floatOrNull ?: return current.value
    val progress = ((elapsed - current.time) / (next.time - current.time)).coerceIn(0f, 1f)
    return JsonPrimitive(from + (to - from) * progress)
}

private fun TimelineParameters.multiplier(parameter: String?): Float =
    parameter?.let { (this[it] as? JsonPrimitive)?.floatOrNull }
        ?.takeIf { it.isFinite() && it >= 0f } ?: 1f

private fun JsonElement.toCoordinate(): Coordinate? {
    val value = this as? JsonObject ?: return null
    val x = value["x"]?.jsonPrimitive?.intOrNull ?: return null
    val y = value["y"]?.jsonPrimitive?.intOrNull ?: return null
    val z = value["z"]?.jsonPrimitive?.intOrNull ?: return null
    return Coordinate(x, y, z)
}
