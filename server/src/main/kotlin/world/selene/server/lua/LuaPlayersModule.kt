package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register
import world.selene.server.network.NetworkServer
import world.selene.server.network.NetworkClientImpl

class LuaPlayersModule(private val signals: ServerLuaSignals, private val networkServer: NetworkServer) : LuaModule {
    override val name = "selene.players"

    override fun register(table: LuaValue) {
        table.set("PlayerQueued", signals.playerQueued)
        table.set("PlayerDequeued", signals.playerDequeued)
        table.set("PlayerJoined", signals.playerJoined)
        table.set("PlayerLeft", signals.playerLeft)
        table.register("GetOnlinePlayers", this::luaGetOnlinePlayers)
    }

    private fun luaGetOnlinePlayers(lua: Lua): Int {
        val players = networkServer.clients
            .filterIsInstance<NetworkClientImpl>()
            .map { it.player }

        lua.push(players, Lua.Conversion.FULL)
        return 1
    }

}
