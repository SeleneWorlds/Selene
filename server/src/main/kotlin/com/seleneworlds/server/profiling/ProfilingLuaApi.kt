package com.seleneworlds.server.profiling

import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** Trusted server scripts may start bounded captures and optionally name deeper scopes. */
class ProfilingLuaApi(private val profiler: ServerProfiler) : LuaModule {
    override val name = "selene.profiler"

    override fun register(table: LuaValue) {
        table.register("startCapture", ::startCapture)
        table.register("stopCapture", ::stopCapture)
        table.register("getSummary", ::getSummary)
        table.register("measure", ::measure)
    }

    private fun startCapture(lua: Lua): Int {
        lua.push(profiler.startCapture(lua.checkInt(1)))
        return 1
    }

    private fun stopCapture(lua: Lua): Int {
        val path = profiler.stopCapture()
        if (path == null) lua.pushNil() else lua.push(path)
        return 1
    }

    private fun getSummary(lua: Lua): Int {
        lua.push(profiler.getSummary(), Lua.Conversion.FULL)
        return 1
    }

    /** measure(name, callback, ...) preserves callback arguments and return values. */
    private fun measure(lua: Lua): Int {
        val scope = ServerProfiler.Scope("lua scope", lua.checkString(1),
            BundleExecutionContext.currentBundle?.manifest?.name.orEmpty(), lua.getCallerInfo().scriptTrace())
        lua.checkType(2, Lua.LuaType.FUNCTION)
        lua.remove(1)
        profiler.measure(scope) { lua.xpCall(lua.top - 1, -1) }
        return lua.top
    }
}
