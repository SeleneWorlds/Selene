package com.seleneworlds.client.timeline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
sealed class TimelineEvent {
    abstract val time: Float
    abstract val keys: Map<String, List<TimelineKeyframe>>
}

@Serializable
data class TimelineKeyframe(
    val time: Float,
    val value: JsonPrimitive,
    val interpolation: TimelineKeyInterpolation = TimelineKeyInterpolation.LINEAR
) {
    init {
        require(time >= 0f && time.isFinite()) { "key time must be a non-negative finite number" }
    }
}

@Serializable
enum class TimelineKeyInterpolation {
    @SerialName("linear") LINEAR,
    @SerialName("step") STEP
}

@Serializable
@SerialName("visual_animation")
data class VisualAnimationTimelineEvent(
    override val time: Float = 0f,
    val visual: String,
    val duration: Float? = null,
    val position: String = "position",
    override val keys: Map<String, List<TimelineKeyframe>> = emptyMap()
) : TimelineEvent()

@Serializable
@SerialName("particle_system")
data class ParticleSystemTimelineEvent(
    override val time: Float = 0f,
    val particle: String,
    val space: ParticleSystemSpace = ParticleSystemSpace.WORLD,
    val position: String = "position",
    val emissionRateMultiplier: String? = null,
    override val keys: Map<String, List<TimelineKeyframe>> = emptyMap()
) : TimelineEvent()

/** Plays a client sound when the timeline reaches this event. */
@Serializable
@SerialName("sound")
data class SoundTimelineEvent(
    override val time: Float = 0f,
    val sound: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    override val keys: Map<String, List<TimelineKeyframe>> = emptyMap()
) : TimelineEvent() {
    init {
        require(sound.isNotBlank()) { "sound must not be blank" }
        require(volume in 0f..1f) { "volume must be between 0 and 1" }
        require(pitch > 0f && pitch.isFinite()) { "pitch must be a positive finite number" }
    }
}

/** A solid color drawn over the game world for a configurable amount of time. */
@Serializable
@SerialName("screen_overlay")
data class ScreenOverlayTimelineEvent(
    override val time: Float = 0f,
    val duration: Float,
    val color: String = "#ffffff",
    val alpha: Float = 1f,
    override val keys: Map<String, List<TimelineKeyframe>> = emptyMap()
) : TimelineEvent() {
    init {
        require(duration > 0f && duration.isFinite()) { "duration must be a positive finite number" }
        require(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$").matches(color)) {
            "color must be #RRGGBB or #RRGGBBAA"
        }
        require(alpha in 0f..1f) { "alpha must be between 0 and 1" }
        require(keys.values.flatten().all { it.time <= duration }) { "keys must fall within the event duration" }
    }
}

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
