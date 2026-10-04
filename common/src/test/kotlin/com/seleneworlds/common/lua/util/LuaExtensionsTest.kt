package com.seleneworlds.common.lua.util

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaDebugModule
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LuaExtensionsTest {

    @Test
    fun `locale conversion accepts underscore and hyphen separators`() {
        val luaManager = LuaManager(LuaPackageModule())
        try {
            luaManager.lua.push("en_US")
            assertEquals(Locale.US, luaManager.lua.toLocale(-1))
            luaManager.lua.pop(1)

            luaManager.lua.push("de-DE")
            assertEquals(Locale.GERMANY, luaManager.lua.toLocale(-1))
        } finally {
            luaManager.lua.close()
        }
    }

    @Test
    fun `xpcall returns values and forwards arguments`() {
        val luaManager = LuaManager(LuaPackageModule())
        try {
            luaManager.lua.load(
                LuaManager.loadBuffer("return xpcall(function(a, b) return a + b, a * b end, 6, 7)"),
                "xpcall_success_test"
            )
            luaManager.lua.pCall(0, 3)

            assertTrue(luaManager.lua.toBoolean(-3))
            assertEquals(13, luaManager.lua.toInteger(-2))
            assertEquals(42, luaManager.lua.toInteger(-1))
        } finally {
            luaManager.lua.close()
        }
    }

    @Test
    fun `xpcall returns engine formatted traceback`() {
        val luaManager = LuaManager(LuaPackageModule())
        try {
            val debug = LuaDebugModule()
            debug.initialize(luaManager)
            luaManager.lua.push(luaManager.lua.newTable { debug.register(this) })
            luaManager.lua.setGlobal("debug")
            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local function inner()
                        error("something went wrong")
                    end
                    local function outer()
                        inner()
                    end
                    return xpcall(outer)
                    """.trimIndent()
                ),
                "xpcall_error_test"
            )
            luaManager.lua.pCall(0, 2)

            assertFalse(luaManager.lua.toBoolean(-2))
            val message = luaManager.lua.toString(-1)!!
            assertTrue(message.contains("something went wrong"), message)
            assertTrue(message.contains("stack traceback:"), message)
            assertTrue(message.contains("in upvalue 'inner'"), message)
            assertTrue(message.contains("in function <[xpcall_error_test]"), message)
            assertFalse(message.contains("[string \""), message)
        } finally {
            luaManager.lua.close()
        }
    }
}
