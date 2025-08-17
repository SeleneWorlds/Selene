package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.RegistryProvider
import world.selene.common.data.TileDefinition

class LuaRegistriesModule(
    private val registryProvider: RegistryProvider
) : LuaModule {
    override val name = "selene.registries"

    override fun register(table: LuaValue) {
        table.register("FindByMetadata", this::luaFindByMetadata)
    }

    private fun luaFindByMetadata(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val key = lua.checkString(2)
        val value = lua.checkString(3)

        val registry = registryProvider.getRegistry(registryName)
        if (registry == null) {
            return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        }

        for ((entryName, entryData) in registry.getAll()) {
            val metadata = getMetadata(entryData, key)
            if (metadata == value) {
                lua.push(RegistryObjectLuaProxy(entryName, entryData), Lua.Conversion.NONE)
                return 1
            }
        }

        return 0
    }

    class RegistryObjectLuaProxy(val name: String, val definition: Any) {
        val Name: String get() = name

        fun GetMetadata(lua: Lua): Int {
            val key = lua.checkString(2)
            val value = getMetadata(definition, key)
            if (value != null) {
                lua.push(value)
                return 1
            }
            return 0
        }
    }

    companion object {
        fun getMetadata(element: Any, key: String): String? {
            return when (element) {
                is TileDefinition -> {
                    element.metadata[key]
                }

                else -> null
            }
        }
    }
}
