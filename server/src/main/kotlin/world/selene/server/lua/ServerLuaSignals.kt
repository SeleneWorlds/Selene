package world.selene.server.lua

import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaSignal

class ServerLuaSignals(luaManager: LuaManager) {
    val serverStarted = LuaSignal(luaManager)
    val serverReloaded = LuaSignal(luaManager)
    val playerQueued = LuaSignal(luaManager)
    val playerDequeued = LuaSignal(luaManager)
    val playerJoined = LuaSignal(luaManager)
    val playerLeft = LuaSignal(luaManager)
    val entitySteppedOnTile = LuaSignal(luaManager)
}