package com.seleneworlds.server.script

import org.slf4j.Logger
import com.seleneworlds.server.profiling.ServerProfiler
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.server.entities.EntityApi

class ServerEntityLuaScript(
    private val module: String,
    private val luaManager: LuaManager,
    private val logger: Logger,
    private val profiler: ServerProfiler
) : ServerEntityScript, ScriptTrace {

    override fun initialize(entity: EntityApi, data: ObservableMap): Boolean {
        return invokeIfPresent("Initialize") { lua ->
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data, Lua.Conversion.NONE)
            lua.xpCall(2, 0, this)
        }
    }

    override fun tick(entity: EntityApi, data: ObservableMap, delta: Float): Boolean {
        return invokeIfPresent("TickEntity") { lua ->
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data, Lua.Conversion.NONE)
            lua.push(delta)
            lua.xpCall(3, 0, this)
        }
    }

    private val scopes = listOf("Initialize", "TickEntity").associateWith {
        ServerProfiler.Scope("entity script", "$module.$it", source = module)
    }

    private fun invokeIfPresent(functionName: String, body: (Lua) -> Unit): Boolean {
        val lua = luaManager.lua
        return try {
            profiler.measure(scopes.getValue(functionName)) {
                lua.getGlobal("require")
                lua.push(module)
                lua.pCall(1, 1)
                lua.getField(-1, functionName)
                if (!lua.isFunction(-1)) {
                    lua.pop(2)
                    return@measure true
                }
                body(lua)
                lua.pop(1)
                true
            }
        } catch (e: LuaException) {
            logger.error("Lua error in server entity script {}", module, e)
            lua.top = 0
            false
        }
    }

    override fun scriptTrace(): String {
        return "[server entity script \"$module\"]"
    }
}
