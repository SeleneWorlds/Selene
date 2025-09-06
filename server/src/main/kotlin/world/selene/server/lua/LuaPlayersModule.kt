package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.register
import world.selene.server.network.NetworkClientImpl
import world.selene.server.network.NetworkServer

/**
 * Player management and player-related signals.
 */
@Suppress("SameReturnValue")
class LuaPlayersModule(signals: ServerLuaSignals, private val networkServer: NetworkServer) : LuaModule {
    override val name = "selene.players"

    /**
     * Fired when a player queues to join the server.
     */
    private val playerQueued: Signal = signals.playerQueued

    /**
     * Fired when a player leaves the queue.
     */
    private val playerDequeued: Signal = signals.playerDequeued

    /**
     * Fired when a player successfully joins the server.
     */
    private val playerJoined: Signal = signals.playerJoined

    /**
     * Fired when a player disconnects from the server.
     */
    private val playerLeft: Signal = signals.playerLeft

    override fun register(table: LuaValue) {
        table.set("PlayerQueued", playerQueued)
        table.set("PlayerDequeued", playerDequeued)
        table.set("PlayerJoined", playerJoined)
        table.set("PlayerLeft", playerLeft)
        table.register("GetOnlinePlayers", this::luaGetOnlinePlayers)
    }

    /**
     * Returns a list of all currently online players.
     *
     * ```signatures
     * GetOnlinePlayers() -> table[Player]
     * ```
     */
    private fun luaGetOnlinePlayers(lua: Lua): Int {
        val players = networkServer.clients
            .filterIsInstance<NetworkClientImpl>()
            .map { it.player }

        lua.push(players, Lua.Conversion.FULL)
        return 1
    }

}
