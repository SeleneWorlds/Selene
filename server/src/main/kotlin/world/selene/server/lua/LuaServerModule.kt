package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule

class LuaServerModule(private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.server"

    override fun register(table: LuaValue) {
        table.set("ServerStarted", signals.serverStarted)
        table.set("ServerReloaded", signals.serverReloaded)
    }
}
