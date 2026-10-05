package com.seleneworlds.client.particles

import com.seleneworlds.client.timeline.ParticleSystemTimelineEvent
import com.seleneworlds.client.timeline.TimelineDefinition
import com.seleneworlds.client.timeline.ParticleSystemSpace
import com.seleneworlds.common.serialization.seleneJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ParticleSystemFormatTest {
    @Test
    fun `decodes portable particle definition`() {
        val definition = seleneJson.decodeFromString<ParticleSystemDefinition>(
            """
            {
              "texture": "example:particles/spark.png",
              "lifetime": { "min": 0.2, "max": 0.6 },
              "frequency": 0.05,
              "emitterLifetime": 0.4,
              "speed": { "start": 80, "end": 10 },
              "scale": { "start": 1, "end": 0 },
              "alpha": { "start": 1, "end": 0 },
              "color": { "start": "#ffffff", "end": "#ff8800" },
              "rotation": { "min": -30, "max": 30 },
              "spawn": { "type": "rectangle", "width": 8, "height": 4 }
            }
            """.trimIndent()
        )

        assertEquals("example:particles/spark.png", definition.texture)
        assertEquals(0.05f, definition.frequency)
        assertIs<ParticleSpawn.Rectangle>(definition.spawn)
    }

    @Test
    fun `decodes particle timeline event`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """
            { "events": [{ "type": "particle_system", "time": 0.25,
              "particle": "example:sparks", "position": "target" }] }
            """.trimIndent()
        )

        val event = assertIs<ParticleSystemTimelineEvent>(timeline.events.single())
        assertEquals("example:sparks", event.particle)
        assertEquals("target", event.position)
    }

    @Test
    fun `screen particle event does not need a position parameter`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{ "events": [{ "type": "particle_system", "particle": "example:rain", "space": "screen" }] }"""
        )

        val event = assertIs<ParticleSystemTimelineEvent>(timeline.events.single())
        assertEquals(ParticleSystemSpace.SCREEN, event.space)
    }

    @Test
    fun `omitted emitter lifetime creates an indefinite system`() {
        val definition = seleneJson.decodeFromString<ParticleSystemDefinition>(
            """{
              "texture": "example:rain.png",
              "lifetime": { "min": 1, "max": 2 },
              "frequency": 0.1
            }"""
        )

        assertNull(definition.emitterLifetime)
    }
}
