package world.selene.server.lua

import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaSignal

class ServerLuaSignals(luaManager: LuaManager) {
    val serverStarted = LuaSignal(luaManager, "serverStarted")
    val serverReloaded = LuaSignal(luaManager, "serverReloaded")
    val playerQueued = LuaSignal(luaManager, "playerQueued")
    val playerDequeued = LuaSignal(luaManager, "playerDequeued")
    val playerJoined = LuaSignal(luaManager, "playerJoined")
    val playerLeft = LuaSignal(luaManager, "playerLeft")
    val entitySteppedOnTile = LuaSignal(luaManager, "entitySteppedOnTile")
}