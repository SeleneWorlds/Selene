package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.server.data.ServerCustomData

class LuaServerModule(
    private val signals: ServerLuaSignals,
    private val serverCustomData: ServerCustomData
) : LuaModule {
    override val name = "selene.server"

    override fun register(table: LuaValue) {
        table.set("ServerStarted", signals.serverStarted)
        table.set("ServerReloaded", signals.serverReloaded)
        table.set("CustomData", serverCustomData.customData)
    }

}
