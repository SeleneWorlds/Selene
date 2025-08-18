package world.selene.client.lua

import world.selene.common.lua.LuaManager
import world.selene.common.lua.Signal

class ClientLuaSignals(luaManager: LuaManager) {
    val gamePreTick = Signal("gamePreTick")
    val mapChunkChanged = Signal("mapChunkChanged")
    val cameraCoordinateChanged = Signal("cameraCoordinateChanged")
}