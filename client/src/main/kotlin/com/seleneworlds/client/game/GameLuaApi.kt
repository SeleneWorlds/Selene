package com.seleneworlds.client.game

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ScriptTrace

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

    fun luaSetWindowAspectRatio(lua: Lua): Int {
        api.setWindowAspectRatio(lua.checkInt(1), lua.checkInt(2))
        return 0
    }

    fun luaClearWindowAspectRatio(@Suppress("UNUSED_PARAMETER") lua: Lua): Int {
        api.clearWindowAspectRatio()
        return 0
    }

    override fun register(table: LuaValue) {
        table.register("SetWindowAspectRatio", ::luaSetWindowAspectRatio)
        table.register("ClearWindowAspectRatio", ::luaClearWindowAspectRatio)
        table.set("PreTick", gamePreTick)
    }
}
