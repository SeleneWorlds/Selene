package world.selene.server.lua

import world.selene.common.lua.Signal

class ServerLuaSignals {
    val serverStarted = Signal("serverStarted")
    val serverReloaded = Signal("serverReloaded")
    val playerQueued = Signal("playerQueued")
    val playerDequeued = Signal("playerDequeued")
    val playerJoined = Signal("playerJoined")
    val playerLeft = Signal("playerLeft")
    val entitySteppedOnTile = Signal("entitySteppedOnTile")
}