package world.selene.common.lua

import party.iroiro.luajava.Lua

interface LuaMetatable {
    fun luaCall(lua: Lua): Int = lua.error(NotImplementedError())
    fun luaGet(lua: Lua): Int = lua.error(NotImplementedError())
    fun luaSet(lua: Lua): Int = lua.error(NotImplementedError())
    fun luaToString(): String = toString()
    fun luaEquals(lua: Lua): Boolean = this == lua.toObject(2)
    fun luaTypeName(): String = javaClass.simpleName
}