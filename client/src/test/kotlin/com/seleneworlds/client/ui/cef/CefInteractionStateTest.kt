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
    fun `reports whether bundle UI has published regions`() {
        val state = CefInteractionState()
        assertFalse(state.hasUi())
        state.update(listOf(CefHitRegion(0, 0, 1, 1)), false)
        assertTrue(state.hasUi())
    }
}
