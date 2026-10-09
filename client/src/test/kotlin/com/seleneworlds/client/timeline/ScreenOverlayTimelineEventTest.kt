package com.seleneworlds.client.timeline

import com.seleneworlds.common.serialization.seleneJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonPrimitive

class ScreenOverlayTimelineEventTest {
    @Test
    fun `textured fog overlay can persist until stopped`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{"events":[{"type":"screen_overlay",
                "texture":"client/textures/example/ui/fog_overlay.png",
                "alphaMultiplier":"fogDensity"}]}"""
        )
        val event = assertIs<ScreenOverlayTimelineEvent>(timeline.events.single())
        assertEquals(null, event.duration)
        assertEquals("client/textures/example/ui/fog_overlay.png", event.texture)
        assertEquals("fogDensity", event.alphaMultiplier)
        assertEquals(1f, event.alpha)
    }

    @Test
    fun `decodes a screen overlay event`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{
                "events": [{
                    "type": "screen_overlay",
                    "time": 0.25,
                    "duration": 2.0,
                    "color": "#20c040",
                    "alpha": 0.4,
                    "keys": {
                        "alpha": [
                            { "time": 0.0, "value": 0.0 },
                            { "time": 0.1, "value": 0.4 },
                            { "time": 1.5, "value": 0.4 },
                            { "time": 2.0, "value": 0.0 }
                        ]
                    }
                }]
            }"""
        )

        val event = assertIs<ScreenOverlayTimelineEvent>(timeline.events.single())
        assertEquals(0.25f, event.time)
        assertEquals(2f, event.duration)
        assertEquals("#20c040", event.color)
        assertEquals(0.4f, event.alpha)
        assertEquals(4, event.keys.getValue("alpha").size)
    }

    @Test
    fun `numeric keys interpolate linearly`() {
        val event = ScreenOverlayTimelineEvent(
            duration = 2f,
            alpha = 0.5f,
            keys = mapOf(
                "alpha" to listOf(
                    TimelineKeyframe(0f, JsonPrimitive(0f)),
                    TimelineKeyframe(0.5f, JsonPrimitive(0.5f)),
                    TimelineKeyframe(2f, JsonPrimitive(0f))
                )
            )
        )

        assertEquals(0f, event.keyedFloat("alpha", event.alpha, 0f), 0.0001f)
        assertEquals(0.25f, event.keyedFloat("alpha", event.alpha, 0.25f), 0.0001f)
        assertEquals(0.5f, event.keyedFloat("alpha", event.alpha, 0.5f), 0.0001f)
        assertEquals(0f, event.keyedFloat("alpha", event.alpha, 2f), 0.0001f)
    }

    @Test
    fun `step keys hold their value until the next key`() {
        val event = ScreenOverlayTimelineEvent(
            duration = 2f,
            color = "#ffffff",
            keys = mapOf(
                "color" to listOf(
                    TimelineKeyframe(0.5f, JsonPrimitive("#00ff00"), TimelineKeyInterpolation.STEP),
                    TimelineKeyframe(1.5f, JsonPrimitive("#ff0000"))
                )
            )
        )

        assertEquals("#ffffff", event.keyedString("color", event.color, 0.25f))
        assertEquals("#00ff00", event.keyedString("color", event.color, 1f))
        assertEquals("#ff0000", event.keyedString("color", event.color, 1.5f))
    }

    @Test
    fun `rejects invalid overlay definitions`() {
        assertFailsWith<IllegalArgumentException> { ScreenOverlayTimelineEvent(texture = "") }
        assertFailsWith<IllegalArgumentException> { ScreenOverlayTimelineEvent(alphaMultiplier = "") }
        assertFailsWith<IllegalArgumentException> { ScreenOverlayTimelineEvent(duration = 0f) }
        assertFailsWith<IllegalArgumentException> { ScreenOverlayTimelineEvent(duration = 1f, color = "green") }
        assertFailsWith<IllegalArgumentException> { ScreenOverlayTimelineEvent(duration = 1f, alpha = 1.1f) }
        assertFailsWith<IllegalArgumentException> {
            ScreenOverlayTimelineEvent(
                duration = 1f,
                keys = mapOf("alpha" to listOf(TimelineKeyframe(2f, JsonPrimitive(0f))))
            )
        }
    }
}
