package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.util.xpCall

/**
 * Server management and server-related events.
 */
class ServerLuaApi(private val api: ServerApi) : LuaModule {
    override val name = "selene.server"

    val serverStarted = LuaEventSink(ServerEvents.ServerStarted.EVENT) { callback: LuaValue, trace: LuaTrace ->
        ServerEvents.ServerStarted {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    val serverReloaded = LuaEventSink(ServerEvents.ServerReloaded.EVENT) { callback: LuaValue, trace: LuaTrace ->
        ServerEvents.ServerReloaded {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    override fun register(table: LuaValue) {
        table.set("ServerStarted", serverStarted)
        table.set("ServerReloaded", serverReloaded)
        table.set("CustomData", api.customData)
    }
}
