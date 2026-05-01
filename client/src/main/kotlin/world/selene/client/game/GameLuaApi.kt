package world.selene.client.game

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.util.xpCall

/**
 * Game lifecycle events.
 */
class GameLuaApi(private val api: GameApi) : LuaModule {
    override val name = "selene.game"

    val gamePreTick = LuaEventSink(ClientEvents.GamePreTick.EVENT) { callback: LuaValue, trace: LuaTrace ->
        ClientEvents.GamePreTick {
            val lua = callback.state()
            lua.push(callback)
            lua.xpCall(0, 0, trace)
        }
    }

    override fun register(table: LuaValue) {
        table.set("PreTick", gamePreTick)
    }
}
