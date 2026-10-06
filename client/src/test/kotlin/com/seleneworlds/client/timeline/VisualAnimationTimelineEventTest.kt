package com.seleneworlds.client.timeline

import com.seleneworlds.common.serialization.seleneJson
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VisualAnimationTimelineEventTest {
    @Test
    fun `decodes alpha keys and elevation behavior`() {
        val timeline = seleneJson.decodeFromString<TimelineDefinition>(
            """{
                "events": [{
                    "type": "visual_animation",
                    "visual": "example:cursor",
                    "duration": 0.1,
                    "ignoresElevation": true,
                    "keys": {
                        "alpha": [
                            { "time": 0.0, "value": 1.0 },
                            { "time": 0.1, "value": 0.0 }
                        ]
                    }
                }]
            }"""
        )

        val event = assertIs<VisualAnimationTimelineEvent>(timeline.events.single())
        assertTrue(event.ignoresElevation)
        assertEquals(0.5f, event.keyedFloat("alpha", 1f, 0.05f), 0.0001f)
        assertEquals(JsonPrimitive(0f), event.keys.getValue("alpha").last().value)
    }
}
