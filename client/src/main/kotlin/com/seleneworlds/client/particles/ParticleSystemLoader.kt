package com.seleneworlds.client.particles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEmitter
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array

/** Converts Selene's portable particle subset to a configured libGDX ParticleEffect. */
class ParticleSystemLoader {
    fun load(definition: ParticleSystemDefinition, texture: Texture): ParticleEffect {
        require(definition.frequency > 0f)
        require(definition.lifetime.min > 0f && definition.lifetime.max >= definition.lifetime.min)
        val emitter = ParticleEmitter().apply {
            name = definition.identifier.toString()
            setMaxParticleCount(definition.maxParticles)
            setMinParticleCount(0)
            setContinuous(definition.emitterLifetime == null)
            setAdditive(definition.additive)
            setCleansUpBlendFunction(true)
            setSprites(Array.with(Sprite(texture)))
            definition.emitterLifetime?.let { getDuration().setLow(it * 1000f) }
            emission.configureConstant(1f / definition.frequency)
            life.configureRange(definition.lifetime.min * 1000f, definition.lifetime.max * 1000f)
            velocity.configureTransition(definition.speed, definition.speedMinimumMultiplier)
            angle.configureRange(definition.angle.min, definition.angle.max)
            rotation.configureRange(definition.rotation.min, definition.rotation.max)
            xScale.configureTransition(
                ParticleTransition(definition.scale.start * texture.width, definition.scale.end * texture.width),
                definition.scaleMinimumMultiplier
            )
            yScale.configureTransition(
                ParticleTransition(definition.scale.start * texture.height, definition.scale.end * texture.height),
                definition.scaleMinimumMultiplier
            )
            transparency.configureTransition(definition.alpha, 1f)
            tint.setColors(floatArrayOf(*parseColor(definition.color.start), *parseColor(definition.color.end)))
            tint.setTimeline(floatArrayOf(0f, 1f))
            configureSpawn(this, definition.spawn)
        }
        return ParticleEffect().apply { emitters.add(emitter) }
    }

    private fun configureSpawn(emitter: ParticleEmitter, spawn: ParticleSpawn) {
        when (spawn) {
            ParticleSpawn.Point -> emitter.spawnShape.setShape(ParticleEmitter.SpawnShape.point)
            is ParticleSpawn.Rectangle -> {
                emitter.spawnShape.setShape(ParticleEmitter.SpawnShape.square)
                emitter.spawnWidth.configureConstant(spawn.width)
                emitter.spawnHeight.configureConstant(spawn.height)
            }
        }
    }

    private fun parseColor(value: String): FloatArray = Color.valueOf(value.removePrefix("#")).let {
        floatArrayOf(it.r, it.g, it.b)
    }
}

private fun ParticleEmitter.ScaledNumericValue.configureConstant(value: Float) {
    setActive(true)
    setLow(value)
    setHigh(value)
    setScaling(floatArrayOf(1f))
    setTimeline(floatArrayOf(0f))
}

private fun ParticleEmitter.ScaledNumericValue.configureRange(min: Float, max: Float) {
    setActive(true)
    setLow(0f)
    setHigh(min, max)
    setScaling(floatArrayOf(1f))
    setTimeline(floatArrayOf(0f))
}

private fun ParticleEmitter.ScaledNumericValue.configureTransition(value: ParticleTransition, minimumMultiplier: Float) {
    setActive(true)
    val maximum = maxOf(value.start, value.end)
    setLow(0f)
    setHigh(maximum * minimumMultiplier, maximum)
    setScaling(
        if (maximum == 0f) floatArrayOf(0f, 0f)
        else floatArrayOf(value.start / maximum, value.end / maximum)
    )
    setTimeline(floatArrayOf(0f, 1f))
}
