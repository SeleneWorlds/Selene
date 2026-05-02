package com.seleneworlds.client.game

import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.xpCall

/**
 * Game lifecycle events.
 */
class GameLuaApi(private val api: GameApi) : LuaModule {
    override val name = "selene.game"

    val gamePreTick = LuaEventSink(ClientEvents.GamePreTick.EVENT) { callback: LuaValue, trace: ScriptTrace ->
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
