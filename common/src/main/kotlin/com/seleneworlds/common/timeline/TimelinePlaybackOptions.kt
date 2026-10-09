package com.seleneworlds.common.timeline

import com.seleneworlds.common.serialization.SerializedMap

/** A stable instance ID updates an active playback instead of restarting its events. Time is in seconds. */
data class TimelinePlaybackOptions(val instanceId: String? = null, val transition: Float = 0f) {
    init {
        require(instanceId == null || instanceId.isNotBlank()) { "instanceId must not be blank" }
        require(transition.isFinite() && transition >= 0f) { "transition must be a non-negative finite number" }
    }

    companion object {
        fun from(values: SerializedMap): TimelinePlaybackOptions {
            val id = values["instanceId"]
            require(id == null || id is String) { "instanceId must be a string" }
            val duration = values["transition"]
            require(duration == null || duration is Number) { "transition must be a number" }
            return TimelinePlaybackOptions(id, duration?.toFloat() ?: 0f)
        }
    }
}
