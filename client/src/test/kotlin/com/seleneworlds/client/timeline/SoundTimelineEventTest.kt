package com.seleneworlds.client.timeline

import com.seleneworlds.common.serialization.seleneJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SoundTimelineEventTest {
    @Test
    fun `decodes sound event`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{"events":[{"type":"sound","time":0.25,"sound":"example:thunder","volume":0.8,"pitch":0.9}]}"""
        )

        val event = assertIs<SoundTimelineEvent>(timeline.events.single())
        assertEquals(0.25f, event.time)
        assertEquals("example:thunder", event.sound)
        assertEquals(0.8f, event.volume)
        assertEquals(0.9f, event.pitch)
    }

    @Test
    fun `uses sound defaults`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{"events":[{"type":"sound","sound":"example:thunder"}]}"""
        )

        val event = assertIs<SoundTimelineEvent>(timeline.events.single())
        assertEquals(0f, event.time)
        assertEquals(1f, event.volume)
        assertEquals(1f, event.pitch)
    }

    @Test
    fun `validates sound properties`() {
        assertFailsWith<IllegalArgumentException> { SoundTimelineEvent(sound = "") }
        assertFailsWith<IllegalArgumentException> { SoundTimelineEvent(sound = "example:test", volume = 1.1f) }
        assertFailsWith<IllegalArgumentException> { SoundTimelineEvent(sound = "example:test", pitch = 0f) }
    }
}
