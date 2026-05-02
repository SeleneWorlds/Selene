package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua

interface LuaMetatableProvider {
    fun luaMetatable(lua: Lua): LuaMetatable
}