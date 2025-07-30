package world.selene.server.lua

import world.selene.common.lua.LuaSignal

class ServerLuaSignals {
    val serverStarted = LuaSignal()
    val serverReloaded = LuaSignal()
    val playerQueued = LuaSignal()
    val playerDequeued = LuaSignal()
    val playerJoined = LuaSignal()
    val playerLeft = LuaSignal()
    val entitySteppedOnTile = LuaSignal()
}