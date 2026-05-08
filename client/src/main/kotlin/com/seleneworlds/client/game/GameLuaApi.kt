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

    fun setWindowAspectRatio(lua: Lua): Int {
        api.setWindowAspectRatio(lua.checkInt(1), lua.checkInt(2))
        return 0
    }

    fun clearWindowAspectRatio(@Suppress("UNUSED_PARAMETER") lua: Lua): Int {
        api.clearWindowAspectRatio()
        return 0
    }

    fun setOffscreenRendering(lua: Lua): Int {
        api.setOffscreenRendering(lua.checkInt(1), lua.checkInt(2))
        return 0
    }

    fun setNativeRendering(@Suppress("UNUSED_PARAMETER") lua: Lua): Int {
        api.setNativeRendering()
        return 0
    }

    override fun register(table: LuaValue) {
        table.register("setWindowAspectRatio", ::setWindowAspectRatio)
        table.register("clearWindowAspectRatio", ::clearWindowAspectRatio)
        table.register("setOffscreenRendering", ::setOffscreenRendering)
        table.register("setNativeRendering", ::setNativeRendering)
        table.set("preTick", gamePreTick)
    }
}
