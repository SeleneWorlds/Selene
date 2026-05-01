package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule

/**
 * Server management and server-related signals.
 */
class ServerLuaApi(private val api: ServerApi) : LuaModule {
    override val name = "selene.server"

    override fun register(table: LuaValue) {
        table.set("ServerStarted", api.serverStarted)
        table.set("ServerReloaded", api.serverReloaded)
        table.set("CustomData", api.customData)
    }
}
