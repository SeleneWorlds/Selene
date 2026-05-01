package world.selene.server.players

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall

/**
 * Player management and player-related events.
 */
@Suppress("SameReturnValue")
class PlayersLuaApi(private val api: PlayersApi) : LuaModule {
    override val name = "selene.players"

    override fun register(table: LuaValue) {
        table.set("PlayerQueued", LuaEventSink(PlayerEvents.PlayerQueued.EVENT) { callback, trace ->
            PlayerEvents.PlayerQueued { entry ->
                val lua = callback.state()
                lua.push(callback)
                lua.push(entry, Lua.Conversion.NONE)
                lua.xpCall(1, 0, trace)
                entry.status
            }
        })
        table.set("PlayerDequeued", LuaEventSink(PlayerEvents.PlayerDequeued.EVENT) { callback, trace ->
            PlayerEvents.PlayerDequeued { entry ->
                val lua = callback.state()
                lua.push(callback)
                lua.push(entry, Lua.Conversion.NONE)
                lua.xpCall(1, 0, trace)
            }
        })
        table.set("PlayerJoined", LuaEventSink(PlayerEvents.PlayerJoined.EVENT) { callback, trace ->
            PlayerEvents.PlayerJoined { entry ->
                val lua = callback.state()
                lua.push(callback)
                lua.push(entry, Lua.Conversion.NONE)
                lua.xpCall(1, 0, trace)
            }
        })
        table.set("PlayerLeft", LuaEventSink(PlayerEvents.PlayerLeft.EVENT) { callback, trace ->
            PlayerEvents.PlayerLeft { entry ->
                val lua = callback.state()
                lua.push(callback)
                lua.push(entry, Lua.Conversion.NONE)
                lua.xpCall(1, 0, trace)
            }
        })
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
