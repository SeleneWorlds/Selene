package com.seleneworlds.common.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HotReloadModeTest {

    @Test
    fun parsesBooleanLikeModes() {
        assertEquals(HotReloadMode.OFF, HotReloadMode.parse("false"))
        assertEquals(HotReloadMode.ON, HotReloadMode.parse("true"))
        assertEquals(HotReloadMode.ON, HotReloadMode.parse("on"))
    }

    @Test
    fun parsesEagerMode() {
        assertEquals(HotReloadMode.EAGER, HotReloadMode.parse(" eager "))
    }

    @Test
    fun rejectsUnknownMode() {
        assertFailsWith<IllegalArgumentException> {
            HotReloadMode.parse("maybe")
        }
    }
}
