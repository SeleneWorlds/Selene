package world.selene.server.players

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Player management and player-related signals.
 */
@Suppress("SameReturnValue")
class PlayersLuaApi(private val api: PlayersApi) : LuaModule {
    override val name = "selene.players"

    override fun register(table: LuaValue) {
        // TODO table.set("PlayerQueued", playerQueued)
        // TODO table.set("PlayerDequeued", playerDequeued)
        // TODO table.set("PlayerJoined", playerJoined)
        // TODO table.set("PlayerLeft", playerLeft)
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
        lua.push(api.getOnlinePlayers(), Lua.Conversion.FULL)
        return 1
    }

}
