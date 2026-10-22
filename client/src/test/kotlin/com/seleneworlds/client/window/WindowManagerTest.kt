package com.seleneworlds.client.window

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowManagerTest {
    @Test
    fun uiResolutionDoesNotFollowWorldFramebufferResolution() {
        val manager = WindowManager()
        manager.setOffscreenRendering(2048, 1536)
        assertEquals(2048, manager.viewport.logicalWidth)
        assertEquals(1024, manager.uiViewport.logicalWidth)
        assertEquals(768, manager.uiViewport.logicalHeight)

        manager.updateWindowSize(1600, 900)
        assertEquals(WindowViewport(1200, 900, 200, 0, 1200, 900), manager.uiViewport)

        manager.setNativeRendering()
        assertEquals(WindowViewport(1600, 900, 0, 0, 1600, 900), manager.uiViewport)
    }
}
