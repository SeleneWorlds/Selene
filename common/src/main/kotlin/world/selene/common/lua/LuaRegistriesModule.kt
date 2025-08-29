package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.RegistryProvider

class LuaRegistriesModule(
    private val registryProvider: RegistryProvider
) : LuaModule {
    override val name = "selene.registries"

    override fun register(table: LuaValue) {
        table.register("FindAll", this::luaFindAll)
        table.register("FindByMetadata", this::luaFindByMetadata)
        table.register("FindByName", this::luaFindByName)
    }

    private fun luaFindAll(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        val list = registry.getAll()
        lua.push(list, Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindByMetadata(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val key = lua.checkString(2)
        val value = lua.toAny(3)
            ?: return lua.error(IllegalArgumentException("Value must not be nil"))

        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))

        val found = registry.findByMetadata(key, value)
        if (found != null) {
            lua.push( found.second, Lua.Conversion.NONE)
            return 1
        }
        return 0
    }

    private fun luaFindByName(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val name = lua.checkString(2)
        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        val element = registry.get(name) ?: return 0
        lua.push(element, Lua.Conversion.NONE)
        return 1
    }

}
