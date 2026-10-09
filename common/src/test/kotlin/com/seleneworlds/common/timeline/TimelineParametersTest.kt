package com.seleneworlds.common.timeline

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimelineParametersTest {
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
    private fun TimelineParameters.opacity() = (this["fogDensity"] as JsonPrimitive).double

    @Test
    fun `fade in and out from current value with interruptions`() {
        val parameters = TimelineParameters(json("""{"fogDensity":0}"""))
        parameters.retarget(json("""{"fogDensity":1}"""), 2f)
        assertEquals(0.0, parameters.opacity())
        parameters.update(0.5f)
        assertEquals(0.25, parameters.opacity())
        parameters.retarget(json("""{"fogDensity":0}"""), 1f)
        parameters.update(0.5f)
        assertEquals(0.125, parameters.opacity())
        parameters.update(2f)
        assertEquals(0.0, parameters.opacity())
    }

    @Test
    fun `same target does not restart a transition and omitted parameters survive`() {
        val parameters = TimelineParameters(json("""{"fogDensity":0,"other":4}"""))
        parameters.retarget(json("""{"fogDensity":1}"""), 2f)
        parameters.update(1f)
        parameters.retarget(json("""{"fogDensity":1}"""), 2f)
        parameters.update(1f)
        assertEquals(1.0, parameters.opacity())
        assertEquals(JsonPrimitive(4), parameters["other"])
    }

    @Test
    fun `zero duration cancels transition and nonnumeric values switch immediately`() {
        val parameters = TimelineParameters(json("""{"fogDensity":0,"mode":"clear"}"""))
        parameters.retarget(json("""{"fogDensity":1,"mode":"fog","new":5}"""), 2f)
        assertEquals(JsonPrimitive("fog"), parameters["mode"])
        assertEquals(JsonPrimitive(5), parameters["new"])
        parameters.update(1f)
        parameters.retarget(json("""{"fogDensity":1}"""), 0f)
        assertEquals(1.0, parameters.opacity())
        parameters.update(1f)
        assertEquals(1.0, parameters.opacity())
        parameters.retarget(json("""{"fogDensity":"0"}"""), 2f)
        assertEquals(JsonPrimitive("0"), parameters["fogDensity"])
    }

    @Test
    fun `options and transition duration are validated`() {
        val parameters = TimelineParameters(json("{}"))
        for (duration in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { parameters.retarget(json("{}"), duration) }
            assertFailsWith<IllegalArgumentException> { TimelinePlaybackOptions(transition = duration) }
        }
        assertEquals(TimelinePlaybackOptions("fog", 2f), TimelinePlaybackOptions.from(mapOf("instanceId" to "fog", "transition" to 2)))
        assertFailsWith<IllegalArgumentException> { TimelinePlaybackOptions.from(mapOf("instanceId" to " ")) }
        assertFailsWith<IllegalArgumentException> { TimelinePlaybackOptions.from(mapOf("transition" to "2")) }
    }
}
