package com.seleneworlds.client.particles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
data class ParticleRange(val min: Float, val max: Float)

@Serializable
data class ParticleTransition(val start: Float = 1f, val end: Float = 1f)

@Serializable
data class ParticleColorTransition(val start: String = "#ffffff", val end: String = "#ffffff")

@Serializable
sealed class ParticleSpawn {
    @Serializable @SerialName("point") data object Point : ParticleSpawn()
    @Serializable @SerialName("rectangle") data class Rectangle(val width: Float, val height: Float) : ParticleSpawn()
}

/** Particle feature subset that has equivalent Pixi and libGDX runtime representations. */
@Serializable
data class ParticleSystemDefinition(
    val texture: String,
    val lifetime: ParticleRange,
    val frequency: Float,
    val emitterLifetime: Float,
    val maxParticles: Int = 100,
    val speed: ParticleTransition = ParticleTransition(),
    val speedMinimumMultiplier: Float = 1f,
    val scale: ParticleTransition = ParticleTransition(),
    val scaleMinimumMultiplier: Float = 1f,
    val alpha: ParticleTransition = ParticleTransition(),
    val color: ParticleColorTransition = ParticleColorTransition(),
    val rotation: ParticleRange = ParticleRange(0f, 0f),
    val spawn: ParticleSpawn = ParticleSpawn.Point,
    val additive: Boolean = false,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : MetadataHolder, RegistryAdoptedObject<ParticleSystemDefinition>() {
    init {
        require(lifetime.min > 0f && lifetime.max >= lifetime.min)
        require(frequency > 0f && emitterLifetime > 0f && maxParticles > 0)
        require(speed.start >= 0f && speed.end >= 0f && speedMinimumMultiplier in 0f..1f)
        require(scale.start >= 0f && scale.end >= 0f && scaleMinimumMultiplier in 0f..1f)
        require(alpha.start in 0f..1f && alpha.end in 0f..1f)
        require(rotation.max >= rotation.min)
        require(Regex("^#[0-9a-fA-F]{6}$").matches(color.start))
        require(Regex("^#[0-9a-fA-F]{6}$").matches(color.end))
        if (spawn is ParticleSpawn.Rectangle) require(spawn.width >= 0f && spawn.height >= 0f)
    }
}
