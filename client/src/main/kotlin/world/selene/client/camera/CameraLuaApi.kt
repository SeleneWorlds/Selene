package world.selene.client.camera

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.game.ClientEvents
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaModule
import world.selene.common.script.ScriptTrace
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall

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
