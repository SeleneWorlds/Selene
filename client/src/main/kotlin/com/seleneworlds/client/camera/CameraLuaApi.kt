package com.seleneworlds.client.camera

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall

/**
 * Manage the camera and convert coordinates.
 */
class CameraLuaApi(private val api: CameraApi) : LuaModule {
    override val name = "selene.camera"

    val cameraCoordinateChanged = LuaEventSink(ClientEvents.CameraCoordinateChanged.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ClientEvents.CameraCoordinateChanged { coordinate ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.xpCall(1, 0, trace)
        }
    }

    fun luaGetCoordinate(lua: Lua): Int {
        lua.push(api.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    fun luaSetViewport(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val width = lua.checkInt(3)
        val height = lua.checkInt(4)
        api.setViewport(x, y, width, height)
        return 0
    }

    fun luaScreenToWorld(lua: Lua): Int {
        val worldPos = api.screenToWorld(lua.checkFloat(1), lua.checkFloat(2))
        lua.push(worldPos.x)
        lua.push(worldPos.y)
        return 2
    }

    override fun register(table: LuaValue) {
        table.register("SetViewport", ::luaSetViewport)
        table.register("ScreenToWorld", ::luaScreenToWorld)
        table.register("GetCoordinate", ::luaGetCoordinate)
        table.set("OnCoordinateChanged", cameraCoordinateChanged)
    }
}
