package world.selene.client.lua

import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaSignal

class ClientLuaSignals(luaManager: LuaManager) {
    val gamePreTick = LuaSignal(luaManager, "gamePreTick")
    val mapChunkChanged = LuaSignal(luaManager, "mapChunkChanged")
    val cameraCoordinateChanged = LuaSignal(luaManager, "cameraCoordinateChanged")
}