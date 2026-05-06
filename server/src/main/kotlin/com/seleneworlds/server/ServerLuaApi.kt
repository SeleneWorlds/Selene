package com.seleneworlds.server

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.data.Identifier
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

    private fun getCustomData(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        lua.push(api.getCustomData(identifier), Lua.Conversion.FULL)
        return 1
    }

    private fun setCustomData(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        val value = lua.toObject(2)
        api.setCustomData(identifier, value)
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
        table.register("getCustomData", this::getCustomData)
        table.register("setCustomData", this::setCustomData)
        table.set("serverStarted", serverStarted)
        table.set("serverReloaded", serverReloaded)
    }
}
