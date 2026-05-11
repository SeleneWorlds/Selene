package com.seleneworlds.client.entity

internal class MotionStateTracker(
    private val graceDurationSeconds: Float = DEFAULT_GRACE_DURATION_SECONDS
) {
    private var graceRemainingSeconds = 0f

    fun onMotionQueued() {
        graceRemainingSeconds = graceDurationSeconds
    }

    fun onMotionCompleted() {
        graceRemainingSeconds = graceDurationSeconds
    }

    fun update(delta: Float, hasQueuedMotion: Boolean) {
        if (hasQueuedMotion || graceRemainingSeconds <= 0f) {
            return
        }

        graceRemainingSeconds = (graceRemainingSeconds - delta).coerceAtLeast(0f)
    }

    fun isInMotion(hasQueuedMotion: Boolean): Boolean {
        return hasQueuedMotion || graceRemainingSeconds > 0f
    }

    companion object {
        private const val DEFAULT_GRACE_DURATION_SECONDS = 0.05f
    }
}
