package world.selene.client.camera

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Manage the camera and convert coordinates.
 */
class CameraLuaApi(private val api: CameraApi) : LuaModule {
    override val name = "selene.camera"

    override fun register(table: LuaValue) {
        table.register("SetViewport", api::luaSetViewport)
        table.register("ScreenToWorld", api::luaScreenToWorld)
        table.register("GetCoordinate", api::luaGetCoordinate)
        table.set("OnCoordinateChanged", api.cameraCoordinateChanged)
    }
}
