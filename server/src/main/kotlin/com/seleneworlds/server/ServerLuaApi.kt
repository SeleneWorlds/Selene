package com.seleneworlds.server

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ScriptTrace

/**
 * Server management and server-related events.
 */
class ServerLuaApi(private val api: ServerApi) : LuaModule {
    override val name = "selene.server"
    var customLuaData: LuaValue? = null

    private fun getRuntimeData(lua: Lua): Int {
        val namespace = lua.checkString(1)
        val customLuaData = customLuaData
            ?: lua.newTable().let { lua.get() }.also { this.customLuaData = it }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.rawGet(-2)

        if (lua.isNil(-1)) {
            lua.pop(1) // pop the nil
            lua.newTable() // new table for this namespace
            lua.push(namespace)
            lua.pushValue(-2) // copy the new table
            lua.rawSet(-4)
        }
        lua.remove(-2)
        return 1
    }

    private fun overwriteRuntimeData(lua: Lua): Int {
        val namespace = lua.checkString(1)
        lua.checkType(2, Lua.LuaType.TABLE)
        val customLuaData = customLuaData
            ?: lua.newTable().let { lua.get() }.also { this.customLuaData = it }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.pushValue(2)
        lua.rawSet(-3)
        return 0
    }

    private fun hasRuntimeData(lua: Lua): Int {
        val namespace = lua.checkString(1)
        val customLuaData = customLuaData

        if (customLuaData == null) {
            lua.push(false)
            return 1
        }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.rawGet(-2)
        lua.push(!lua.isNil(-1))
        return 1
    }

    private fun removeRuntimeData(lua: Lua): Int {
        val namespace = lua.checkString(1)
        val customLuaData = customLuaData ?: return 0

        customLuaData.push(lua)
        lua.push(namespace)
        lua.pushNil()
        lua.rawSet(-3)
        return 0
    }

    val serverStarted = LuaEventSink(ServerEvents.ServerStarted.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ServerEvents.ServerStarted {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    val serverReloaded = LuaEventSink(ServerEvents.ServerReloaded.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ServerEvents.ServerReloaded {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    override fun register(table: LuaValue) {
        table.register("getRuntimeData", this::getRuntimeData)
        table.register("overwriteRuntimeData", this::overwriteRuntimeData)
        table.register("hasRuntimeData", this::hasRuntimeData)
        table.register("removeRuntimeData", this::removeRuntimeData)
        table.set("serverStarted", serverStarted)
        table.set("serverReloaded", serverReloaded)
    }
}
