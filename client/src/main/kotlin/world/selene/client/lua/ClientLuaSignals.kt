package world.selene.client.lua

import world.selene.common.lua.Signal

class ClientLuaSignals {
    val gamePreTick = Signal("gamePreTick")
    val mapChunkChanged = Signal("mapChunkChanged")
    val cameraCoordinateChanged = Signal("cameraCoordinateChanged")
}