package world.selene.client.lua

import com.badlogic.gdx.math.Vector3
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.camera.CameraManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkInt
import world.selene.common.lua.register

/**
 * Provides functions for camera control and coordinate conversion.
 */
class LuaCameraModule(
    private val cameraManager: CameraManager,
    private val signals: ClientLuaSignals
) : LuaModule {
    override val name = "selene.camera"

    /**
     * Fired when the camera coordinate changes.
     */
    private val cameraCoordinateChanged: Signal = signals.cameraCoordinateChanged

    override fun register(table: LuaValue) {
        table.register("SetViewport", this::luaSetViewport)
        table.register("ScreenToWorld", this::luaScreenToWorld)
        table.register("GetCoordinate", this::luaGetCoordinate)
        table.set("OnCoordinateChanged", cameraCoordinateChanged)
    }

    /**
     * Returns the current target coordinate of the camera.
     *
     * ```signatures
     * GetCoordinate() -> Coordinate
     * ```
     */
    private fun luaGetCoordinate(lua: Lua): Int {
        lua.push(cameraManager.focusCoordinate, Lua.Conversion.NONE)
        return 1
    }

    /**
     * ```signatures
     * SetViewport(x: number, y: number, width: number, height: number)
     * ```
     */
    private fun luaSetViewport(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val width = lua.checkInt(3)
        val height = lua.checkInt(4)
        cameraManager.setViewport(x, y, width, height)
        return 0
    }

    /**
     * Converts screen coordinates to world coordinates.
     *
     * ```signatures
     * ScreenToWorld(screenX: number, screenY: number) -> worldX: number, worldY: number
     * ```
     */
    private fun luaScreenToWorld(lua: Lua): Int {
        val worldPos = cameraManager.camera.unproject(Vector3(lua.checkFloat(1), lua.checkFloat(2), 0f))
        lua.push(worldPos.x)
        lua.push(worldPos.y)
        return 2
    }
}
