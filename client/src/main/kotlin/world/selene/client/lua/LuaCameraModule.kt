package world.selene.client.lua

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.camera.CameraManager
import world.selene.client.network.NetworkClient
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkInt
import world.selene.common.lua.luaTableToMap
import world.selene.common.lua.register
import world.selene.common.network.packet.CustomPayloadPacket

class LuaCameraModule(private val cameraManager: CameraManager) : LuaModule {
    override val name = "selene.camera"

    override fun register(table: LuaValue) {
        table.register("SetViewport", this::luaSetViewport)
        table.register("ScreenToWorld", this::luaScreenToWorld)
    }

    private fun luaSetViewport(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val width = lua.checkInt(3)
        val height = lua.checkInt(4)
        cameraManager.setViewport(x, y, width, height)
        return 0
    }

    private fun luaScreenToWorld(lua: Lua): Int {
        val worldPos = cameraManager.camera.unproject(Vector3(lua.checkFloat(1), lua.checkFloat(2), 0f))
        lua.push(worldPos.x)
        lua.push(worldPos.y)
        return 2
    }
}
