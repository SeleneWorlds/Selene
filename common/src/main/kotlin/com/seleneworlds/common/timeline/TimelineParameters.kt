package com.seleneworlds.common.timeline

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/** Live playback parameters. Each numeric update behaves like a pair of linear keyframes. */
class TimelineParameters(initial: JsonObject) {
    private data class Transition(val from: Double, val to: Double, val duration: Float, var elapsed: Float = 0f)

    private val values = initial.toMutableMap()
    private val transitions = mutableMapOf<String, Transition>()

    operator fun get(name: String): JsonElement? = values[name]

    /** Omitted parameters are preserved; identical targets do not restart an ongoing transition. */
    fun retarget(parameters: JsonObject, duration: Float) {
        require(duration.isFinite() && duration >= 0f) { "transition must be a non-negative finite number" }
        for ((name, target) in parameters) {
            val from = values[name].number()
            val to = target.number()
            if (duration > 0f && to != null && transitions[name]?.to == to) continue
            transitions.remove(name)
            if (duration > 0f && from != null && to != null && from != to) {
                transitions[name] = Transition(from, to, duration)
            } else {
                values[name] = target
            }
        }
    }

    fun update(delta: Float) {
        val iterator = transitions.iterator()
        while (iterator.hasNext()) {
            val (name, transition) = iterator.next()
            transition.elapsed = (transition.elapsed + delta).coerceAtMost(transition.duration)
            val progress = transition.elapsed / transition.duration
            values[name] = JsonPrimitive(transition.from + (transition.to - transition.from) * progress)
            if (progress >= 1f) iterator.remove()
        }
    }
}

private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)
    ?.takeUnless { it.isString }?.doubleOrNull?.takeIf { it.isFinite() }
