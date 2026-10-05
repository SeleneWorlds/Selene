package com.seleneworlds.client.timeline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
sealed class TimelineEvent { abstract val time: Float }

@Serializable
@SerialName("visual_animation")
data class VisualAnimationTimelineEvent(
    override val time: Float = 0f,
    val visual: String,
    val duration: Float? = null,
    val position: String = "position"
) : TimelineEvent()

@Serializable
@SerialName("particle_system")
data class ParticleSystemTimelineEvent(
    override val time: Float = 0f,
    val particle: String,
    val space: ParticleSystemSpace = ParticleSystemSpace.WORLD,
    val position: String = "position"
) : TimelineEvent()

@Serializable
enum class ParticleSystemSpace {
    @SerialName("world") WORLD,
    @SerialName("screen") SCREEN
}

@Serializable
data class TimelineDefinition(
    val events: List<TimelineEvent>,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : MetadataHolder, RegistryAdoptedObject<TimelineDefinition>()
