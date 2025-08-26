package world.selene.common.lua

import party.iroiro.luajava.Lua

class ManagedLuaTable(val map: MutableMap<Any, Any> = mutableMapOf()) : LuaMetatable, Map<Any, Any> by map {

    override fun luaGet(lua: Lua): Int {
        val key = when {
            lua.isString(2) -> lua.toString(2)!!
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            else -> return 0
        }
        val value = map[key]
        if (value != null) {
            lua.push(value, Lua.Conversion.NONE)
            return 1
        }
        return 0
    }

    override fun luaSet(lua: Lua): Int {
        val key = when {
            lua.isString(2) -> lua.toString(2)!!
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            else -> return lua.pushNil().let { 1 }
        }
        val value = lua.toAny(3) ?: return lua.pushNil().let { 1 }
        map[key] = value
        return 1
    }

    companion object {
        val luaMeta = LuaMappedMetatable(ManagedLuaTable::class) {

        }
    }
}