package world.selene.common.lua

import com.fasterxml.jackson.databind.JsonNode
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.MetadataHolder
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
        if (registry == null) {
            return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        }
        val list = registry.getAll().map { TransientRegistryObject(it.key, it.value) }
        lua.push(list, Lua.Conversion.FULL)
        return 1
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
                lua.push(TransientRegistryObject(entryName, entryData), Lua.Conversion.NONE)
                return 1
            }
        }

        return 0
    }

    private fun luaFindByName(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val name = lua.checkString(2)
        val registry = registryProvider.getRegistry(registryName)
        if (registry == null) {
            return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        }
        val element = registry.get(name) ?: return 0
        lua.push(TransientRegistryObject(name, element), Lua.Conversion.NONE)
        return 1
    }

    class TransientRegistryObject(val name: String, val element: Any) : LuaMetatableProvider {
        override fun luaMetatable(lua: Lua): LuaMetatable {
            return luaMeta
        }

        companion object {
            val luaMeta = LuaMappedMetatable(TransientRegistryObject::class) {
                readOnly(TransientRegistryObject::name)
                callable("GetMetadata") {
                    val registryObject = it.checkSelf()
                    val key = it.checkString(2)
                    val value = getMetadata(registryObject.element, key)
                    if (value != null) {
                        it.push(value)
                        return@callable 1
                    }
                    0
                }
            }
        }
    }

    companion object {
        fun getMetadata(element: Any, key: String): String? {
            return when (element) {
                is MetadataHolder -> {
                    element.metadata[key]
                }

                is JsonNode -> {
                    element["metadata"]?.get(key)?.asText()
                }

                else -> null
            }
        }
    }
}
