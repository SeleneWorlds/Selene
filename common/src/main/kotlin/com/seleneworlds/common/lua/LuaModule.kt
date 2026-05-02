package com.seleneworlds.common.lua

import party.iroiro.luajava.value.LuaValue

interface LuaModule {
    val name: String
    val registerAsGlobal: Boolean get() = false
    fun register(table: LuaValue)
    fun initialize(luaManager: LuaManager) {}
}