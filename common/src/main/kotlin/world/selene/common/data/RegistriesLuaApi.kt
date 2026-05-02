package world.selene.common.data

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAny

/**
 * Lookup entries in game registries.
 */
class RegistriesLuaApi(private val api: RegistriesApi) : LuaModule {
    override val name = "selene.registries"

    override fun register(table: LuaValue) {
        table.register("FindAll", this::luaFindAll)
        table.register("FindByMetadata", this::luaFindByMetadata)
        table.register("FindByName", this::luaFindByName)
    }

    private fun luaFindAll(lua: Lua): Int {
        lua.push(api.findAll(lua.checkString(1)), Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindByMetadata(lua: Lua): Int {
        val value = lua.toAny(3) ?: return lua.error(IllegalArgumentException("Value must not be nil"))
        val found = api.findByMetadata(lua.checkString(1), lua.checkString(2), value)
        if (found != null) {
            lua.push(found, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaFindByName(lua: Lua): Int {
        val element = api.findByName(lua.checkString(1), lua.checkString(2))
        if (element != null) {
            lua.push(element, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }
}
