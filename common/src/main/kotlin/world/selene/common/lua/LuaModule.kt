package world.selene.common.lua

import party.iroiro.luajava.value.LuaValue

interface LuaModule {
    val name: String
    fun register(table: LuaValue)
    fun initialize(luaManager: LuaManager) {}
}