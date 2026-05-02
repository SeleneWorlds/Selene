package com.seleneworlds.common.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import com.seleneworlds.common.sounds.SoundDefinition

class JsonStringAnyMapSerializerTest {

    @Test
    fun `deserializes numeric sound metadata`() {
        val json = """
            {
              "audio": "client/sounds/illarion/saw.ogg",
              "metadata": {
                "soundId": 11
              }
            }
        """.trimIndent()

        val soundDefinition = seleneJson.decodeFromString<SoundDefinition>(json)

        val soundId = soundDefinition.metadata["soundId"]
        assertIs<Int>(soundId)
        assertEquals(11, soundId)
    }
}
