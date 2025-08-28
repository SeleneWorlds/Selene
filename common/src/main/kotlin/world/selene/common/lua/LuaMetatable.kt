package world.selene.common.lua

import party.iroiro.luajava.Lua

interface LuaMetatable {
    fun luaCall(lua: Lua): Int = lua.throwError("attempt to call a ${luaTypeName()} value")
    fun luaGet(lua: Lua): Int = lua.throwError("attempt to index a ${luaTypeName()} value")
    fun luaSet(lua: Lua): Int = lua.throwError("attempt to index a ${luaTypeName()} value")
    fun luaToString(lua: Lua): String = toString()
    fun luaEquals(lua: Lua): Boolean = lua.toAny(1) == lua.toAny(2)
    fun luaTypeName(): String = javaClass.simpleName
}