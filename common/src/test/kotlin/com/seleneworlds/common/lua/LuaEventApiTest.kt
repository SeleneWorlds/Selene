package com.seleneworlds.common.lua

import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LuaEventApiTest {

    @Test
    fun `lua events can connect and fire with arguments`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val events = EventsLuaApi()
        try {
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable {
                events.register(this)
            })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal("Event")
            luaManager.lua.setField(-2, events.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local event = Event.of("test:event")
                    local calls = 0

                    event:connect(function(message, missing, count)
                        calls = calls + 1
                        firstMessage = message
                        firstMissing = missing == nil
                        firstCount = count
                    end)

                    event:connect(function(message, missing, count)
                        calls = calls + 1
                        secondMessage = message
                        secondMissing = missing == nil
                        secondCount = count
                    end)

                    event:fire("hello", nil, 42)
                    _G.calls = calls
                    """.trimIndent()
                ),
                "lua_event_api_test"
            )
            luaManager.lua.pCall(0, 0)

            luaManager.lua.getGlobal("calls")
            assertEquals(2, luaManager.lua.toInteger(-1).toInt())
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("firstMessage")
            assertEquals("hello", luaManager.lua.toString(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("firstMissing")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("firstCount")
            assertEquals(42, luaManager.lua.toInteger(-1).toInt())
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("secondMessage")
            assertEquals("hello", luaManager.lua.toString(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("secondMissing")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("secondCount")
            assertEquals(42, luaManager.lua.toInteger(-1).toInt())
            luaManager.lua.pop(1)
        } finally {
            luaManager.lua.close()
        }
    }

    @Test
    fun `lua events preserve lua table identity for fired arguments`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val events = EventsLuaApi()
        try {
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable {
                events.register(this)
            })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal("Event")
            luaManager.lua.setField(-2, events.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local event = Event.of("test:event")
                    local payload = { count = 1 }

                    event:connect(function(arg)
                        _G.samePayload = arg == payload
                        arg.count = arg.count + 1
                    end)

                    event:fire(payload)
                    _G.payloadCount = payload.count
                    """.trimIndent()
                ),
                "lua_event_identity_test"
            )
            luaManager.lua.pCall(0, 0)

            luaManager.lua.getGlobal("samePayload")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("payloadCount")
            assertEquals(2, luaManager.lua.toInteger(-1).toInt())
            luaManager.lua.pop(1)
        } finally {
            luaManager.lua.close()
        }
    }

    @Test
    fun `lua events return the same event for the same identifier`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val events = EventsLuaApi()
        try {
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable {
                events.register(this)
            })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal("Event")
            luaManager.lua.setField(-2, events.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local first = Event.of("test:event")
                    local second = Event.of("test:event")
                    local third = Event.of("test:other")

                    _G.sameEvent = first == second
                    _G.differentEvent = first ~= third
                    """.trimIndent()
                ),
                "lua_event_api_cache_test"
            )
            luaManager.lua.pCall(0, 0)

            luaManager.lua.getGlobal("sameEvent")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("differentEvent")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)
        } finally {
            luaManager.lua.close()
        }
    }
}
