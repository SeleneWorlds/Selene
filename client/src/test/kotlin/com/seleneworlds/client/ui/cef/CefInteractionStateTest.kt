package com.seleneworlds.client.ui.cef

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CefInteractionStateTest {
    @Test
    fun `hit regions include their top-left and exclude their bottom-right bounds`() {
        val state = CefInteractionState()
        state.update(listOf(CefHitRegion(10, 20, 30, 40)), false)

        assertTrue(state.isInteractive(10, 20))
        assertTrue(state.isInteractive(39, 59))
        assertFalse(state.isInteractive(40, 60))
        assertFalse(state.isInteractive(9, 20))
    }

    @Test
    fun `publishes editable focus with region updates`() {
        val state = CefInteractionState()
        state.update(emptyList(), true)
        assertTrue(state.hasEditableFocus())
    }

    @Test
    fun `reports whether bundle UI has published state`() {
        val state = CefInteractionState()
        assertFalse(state.hasUi())
        state.update(emptyList(), false)
        assertTrue(state.hasUi())
    }

    @Test
    fun `applies bundle keyboard claims with passthrough taking precedence`() {
        val state = CefInteractionState()
        state.update(emptyList(), true, setOf("Enter"), true, setOf("ArrowUp"))

        assertTrue(state.consumesKey("Enter"))
        assertTrue(state.consumesKey("Escape"))
        assertFalse(state.consumesKey("ArrowUp"))
        assertTrue(state.capturesText())
    }

    @Test
    fun `clear resets published interaction state`() {
        val state = CefInteractionState()
        state.update(listOf(CefHitRegion(0, 0, 10, 10)), true, setOf("Enter"), true)

        state.clear()

        assertFalse(state.hasUi())
        assertFalse(state.isInteractive(1, 1))
        assertFalse(state.consumesKey("Enter"))
        assertFalse(state.capturesText())
    }
}
