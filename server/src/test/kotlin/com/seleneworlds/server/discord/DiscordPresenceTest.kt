package com.seleneworlds.server.discord

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiscordPresenceTest {
    @Test
    fun `publishes initial zero and changes including the last player leaving`() {
        val updates = mutableListOf<String>()
        val presence = DiscordPresence { updates.add(it) }
        listOf(0, 0, 1, 1, 2, 0).forEach(presence::update)
        assertEquals(listOf("0 players online", "1 player online", "2 players online", "0 players online"), updates)
    }

    @Test
    fun `failed publication is retried with the same count`() {
        var fail = true
        val updates = mutableListOf<String>()
        val presence = DiscordPresence {
            if (fail) throw IllegalStateException("Disconnected")
            updates.add(it)
        }
        assertFailsWith<IllegalStateException> { presence.update(3) }
        fail = false
        presence.update(3)
        assertEquals(listOf("3 players online"), updates)
    }
}
