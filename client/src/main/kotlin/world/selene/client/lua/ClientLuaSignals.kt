package world.selene.client.lua

import world.selene.common.lua.LuaSignal

class ClientLuaSignals {
    val gamePreTick = LuaSignal()
    val mapChunkChanged = LuaSignal()
    val cameraCoordinateChanged = LuaSignal()
}