package world.selene.client.lua

import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaSignal

class ClientLuaSignals(luaManager: LuaManager) {
    val gamePreTick = LuaSignal(luaManager)
    val mapChunkChanged = LuaSignal(luaManager)
    val cameraCoordinateChanged = LuaSignal(luaManager)
}