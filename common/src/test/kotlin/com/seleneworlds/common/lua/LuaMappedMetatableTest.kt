package com.seleneworlds.common.lua

import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.checkUserdata
import party.iroiro.luajava.Lua
import kotlin.test.Test
import kotlin.test.assertEquals

class LuaMappedMetatableTest {
    private open class Receiver(val value: Int, private val metatable: LuaMetatable) : LuaMetatableProvider {
        override fun luaMetatable(lua: Lua) = metatable
    }
    private class Child(value: Int, metatable: LuaMetatable) : Receiver(value, metatable)

    private fun read(lua: Lua): Int {
        lua.push(lua.checkUserdata<Receiver>(1).value)
        return 1
    }

    private fun replacement(lua: Lua): Int {
        lua.push(lua.checkUserdata<Receiver>(1).value + 1)
        return 1
    }

    private fun execute(manager: LuaManager, script: String) {
        manager.lua.load(LuaManager.loadBuffer(script), "mapped_metatable_test")
        manager.lua.pCall(0, 0)
        assertEquals(0, manager.lua.top)
    }

    private fun install(manager: LuaManager, metatable: LuaMetatable) {
        manager.lua.push(Receiver(10, metatable), Lua.Conversion.NONE)
        manager.lua.setGlobal("a")
        manager.lua.push(Receiver(30, metatable), Lua.Conversion.NONE)
        manager.lua.setGlobal("b")
    }

    @Test
    fun `method lookups reuse a function without binding the receiver and preserve errors`() {
        val metatable = LuaMappedMetatable(Receiver::class) { callable(::read, "read") }
        val manager = LuaManager(LuaPackageModule())
        try {
            install(manager, metatable)
            execute(manager, """
                local method = a.read
                assert(method == a.read and method == b.read)
                assert(a.missing == nil)
                assert(method(a) == 10 and method(b) == 30)
                for i = 1, 10000 do assert(a:read() == 10) end
                local ok = pcall(function() method({}) end)
                assert(not ok)
                assert(a:read() == 10)
            """.trimIndent())
        } finally {
            manager.lua.close()
        }
    }

    @Test
    fun `coroutines share cached functions with their owning main state`() {
        val metatable = LuaMappedMetatable(Receiver::class) {
            callable(::read, "read")
            callable(::read, "fresh")
        }
        val manager = LuaManager(LuaPackageModule())
        try {
            install(manager, metatable)
            execute(manager, """
                local mainMethod = a.read
                local co = coroutine.create(function()
                    assert(a.read == mainMethod and a:read() == 10)
                    fromCoroutine = a.fresh
                    assert(fromCoroutine(a) == 10)
                    coroutine.yield()
                    assert(b.read == mainMethod and b:read() == 30)
                end)
                local ok, err = coroutine.resume(co)
                assert(ok, err)
                ok, err = coroutine.resume(co)
                assert(ok, err)
                assert(coroutine.status(co) == 'dead')
                assert(a.fresh == fromCoroutine and fromCoroutine(b) == 30)
            """.trimIndent())
        } finally {
            manager.lua.close()
        }
    }

    @Test
    fun `shared metatable keeps caches separate across live and reopened states`() {
        val metatable = LuaMappedMetatable(Receiver::class) { callable(::read, "read") }
        val first = LuaManager(LuaPackageModule())
        val second = LuaManager(LuaPackageModule())
        try {
            try {
                install(first, metatable)
                install(second, metatable)
                execute(first, "saved = a.read; assert(saved(a) == 10)")
                execute(second, "saved = a.read; assert(saved(b) == 30)")
            } finally {
                first.lua.close()
            }
            execute(second, "assert(a.read == saved and saved(a) == 10)")
            val third = LuaManager(LuaPackageModule())
            try {
                install(third, metatable)
                execute(third, "assert(a:read() == 10 and b:read() == 30)")
            } finally {
                third.lua.close()
            }
        } finally {
            second.lua.close()
        }
    }

    @Test
    fun `re-registration refreshes each state while retained old functions still work`() {
        val metatable = LuaMappedMetatable(Receiver::class) { callable(::read, "read") }
        val managers = List(2) { LuaManager(LuaPackageModule()) }
        try {
            for (manager in managers) {
                install(manager, metatable)
                execute(manager, "saved = a.read; assert(saved(a) == 10)")
            }
            metatable.callable(::replacement, "read")
            for (manager in managers) execute(manager, """
                assert(a.read ~= saved)
                assert(a.read == b.read)
                assert(a:read() == 11 and b:read() == 31)
                assert(saved(a) == 10 and saved(b) == 30)
            """.trimIndent())
        } finally {
            managers.forEach { it.lua.close() }
        }
    }

    @Test
    fun `extended mappings preserve aliases and overrides without changing parent cache`() {
        val parent = LuaMappedMetatable(Receiver::class) {
            callable(::read, "aliased")
            getter(::read, "value")
        }
        val child = parent.extend(Child::class) { callable(::replacement, "aliased") }
        val manager = LuaManager(LuaPackageModule())
        try {
            install(manager, parent)
            manager.lua.push(Child(20, child), Lua.Conversion.NONE)
            manager.lua.setGlobal("child")
            execute(manager, """
                assert(a:aliased() == 10 and child:aliased() == 21)
                assert(a.value == 10 and child.value == 20)
                assert(a.aliased ~= child.aliased)
                assert(a.aliased == b.aliased)
            """.trimIndent())
        } finally {
            manager.lua.close()
        }
    }
}
