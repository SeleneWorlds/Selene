package com.seleneworlds.common.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PayloadHandlerRegistryTest {
    @Test
    fun `supports multiple removable handlers for one payload`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val received = mutableListOf<String>()
        val removeFirst = registry.registerHandler("chat") { _, _ -> received += "first" }
        registry.registerHandler("chat") { _, _ -> received += "second" }

        registry.getHandler("chat")?.invoke(Unit, emptyMap())
        removeFirst()
        registry.getHandler("chat")?.invoke(Unit, emptyMap())

        assertEquals(listOf("first", "second", "second"), received)
    }

    @Test
    fun `removes payload entry after its final handler`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val remove = registry.registerHandler("chat") { _, _ -> }
        remove()
        assertNull(registry.getHandler("chat"))
    }
}
