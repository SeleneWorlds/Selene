package world.selene.client.camera

import com.badlogic.gdx.math.Vector3
import party.iroiro.luajava.Lua
import world.selene.client.lua.ClientLuaSignals
import world.selene.common.lua.Signal
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.checkInt

/**
 * Manage the camera and convert coordinates.
 */
@Suppress("SameReturnValue")
class CameraApi(
    private val cameraManager: CameraManager,
    signals: ClientLuaSignals
) {
    val cameraCoordinateChanged: Signal = signals.cameraCoordinateChanged

    fun luaGetCoordinate(lua: Lua): Int {
        lua.push(cameraManager.focusCoordinate, Lua.Conversion.NONE)
        return 1
    }

    fun luaSetViewport(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val width = lua.checkInt(3)
        val height = lua.checkInt(4)
        cameraManager.setViewport(x, y, width, height)
        return 0
    }

    fun luaScreenToWorld(lua: Lua): Int {
        val worldPos = cameraManager.camera.unproject(Vector3(lua.checkFloat(1), lua.checkFloat(2), 0f))
        lua.push(worldPos.x)
        lua.push(worldPos.y)
        return 2
    }
}
