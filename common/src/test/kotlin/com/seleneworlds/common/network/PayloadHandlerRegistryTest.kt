package com.seleneworlds.common.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PayloadHandlerRegistryTest {
    @Test
    fun `supports multiple removable handlers for one payload`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val received = mutableListOf<String>()
        val removeFirst = registry.registerHandler("chat") { _, _ -> received += "first" }
        registry.registerHandler("chat") { _, _ -> received += "second" }

        assertTrue(registry.dispatch("chat", Unit, emptyMap()))
        removeFirst()
        assertTrue(registry.dispatch("chat", Unit, emptyMap()))

        assertEquals(listOf("first", "second", "second"), received)
    }

    @Test
    fun `subscription changes during dispatch apply to the next payload`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val received = mutableListOf<String>()
        lateinit var removeSecond: () -> Unit
        registry.registerHandler("chat") { _, _ ->
            received += "first"
            removeSecond()
        }
        removeSecond = registry.registerHandler("chat") { _, _ -> received += "second" }

        registry.dispatch("chat", Unit, emptyMap())
        registry.dispatch("chat", Unit, emptyMap())

        assertEquals(listOf("first", "second", "first"), received)
    }
}
