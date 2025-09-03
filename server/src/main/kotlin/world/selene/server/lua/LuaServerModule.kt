package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.ManagedLuaTable
import world.selene.common.lua.Signal
import world.selene.server.data.ServerCustomData

/**
 * Provides access to server management and server-related signals.
 */
class LuaServerModule(
    signals: ServerLuaSignals,
    private val serverCustomData: ServerCustomData
) : LuaModule {
    override val name = "selene.server"

    /**
     * Fired when the server starts.
     */
    private val serverStarted: Signal = signals.serverStarted

    /**
     * Fired when the server reloads.
     */
    private val serverReloaded: Signal = signals.serverReloaded

    private val customData: ManagedLuaTable = serverCustomData.customData

    override fun register(table: LuaValue) {
        table.set("ServerStarted", serverStarted)
        table.set("ServerReloaded", serverReloaded)
        table.set("CustomData", customData)
    }

}
