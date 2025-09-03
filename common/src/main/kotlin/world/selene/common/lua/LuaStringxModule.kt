package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaStringxModule : LuaModule {
    override val name: String = "stringx"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("trim", this::luaTrim)
        table.register("startsWith", this::luaStartsWith)
        table.register("endsWith", this::luaEndsWith)
        table.register("removeSuffix", this::luaRemoveSuffix)
        table.register("split", this::luaSplit)
        table.register("substringAfter", this::luaSubstringAfter)
    }

    private fun luaStartsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).startsWith(lua.checkString(2)))
        return 1
    }

    private fun luaEndsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).endsWith(lua.checkString(2)))
        return 1
    }

    private fun luaRemoveSuffix(lua: Lua): Int {
        lua.push(lua.checkString(1).removeSuffix(lua.checkString(2)))
        return 1
    }

    private fun luaTrim(lua: Lua): Int {
        lua.push(lua.checkString(1).trim())
        return 1
    }

    private fun luaSubstringAfter(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.substringAfter(separator)
        lua.push(result)
        return 1
    }

    private fun luaSplit(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.split(separator)
        lua.push(result, Lua.Conversion.FULL)
        return 1
    }

}