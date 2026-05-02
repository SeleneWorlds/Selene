package com.seleneworlds.server.players

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.server.login.LoginQueueEntry
import com.seleneworlds.server.login.LoginQueueEntryLuaApi

/**
 * Player management and player-related events.
 */
class PlayersLuaApi(private val api: PlayersApi) : LuaModule {
    override val name = "selene.players"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(PlayerApi::class, PlayerLuaApi.luaMeta)
        luaManager.defineMetatable(LoginQueueEntry::class, LoginQueueEntryLuaApi.luaMeta)
    }

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
