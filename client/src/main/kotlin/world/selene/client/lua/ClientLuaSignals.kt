package world.selene.client.lua

import world.selene.common.lua.Signal

class ClientLuaSignals {
    val gamePreTick: Signal = Signal("gamePreTick")
    val mapChunkChanged: Signal = Signal("mapChunkChanged")
    val cameraCoordinateChanged: Signal = Signal("cameraCoordinateChanged")
}