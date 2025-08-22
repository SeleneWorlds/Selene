package world.selene.common.lua

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
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
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
        val list = registry.getAll().map { TransientRegistryObject(it.key, it.value) }
        lua.push(list, Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindByMetadata(lua: Lua): Int {
        val registryName = lua.checkString(1)
        val key = lua.checkString(2)
        val value = lua.toObject(3)
            ?: return lua.error(IllegalArgumentException("Value must not be nil"))

        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))

        for ((entryName, entryData) in registry.getAll()) {
            var metadata = getMetadata(entryData, key)
            // Everything is floating point in lua, so we treat metadata as floating point too.
            if (metadata is Number) {
                metadata = metadata.toDouble()
            }
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
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))
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
                        it.push(value, Lua.Conversion.FULL)
                        return@callable 1
                    }
                    0
                }
                callable("GetField") {
                    val registryObject = it.checkSelf()
                    val key = it.checkString(2)
                    if (registryObject.element is JsonNode) {
                        val value = registryObject.element[key]
                        when (value) {
                            is LongNode -> it.push(value.asLong())
                            is IntNode, is ShortNode -> it.push(value.asInt())
                            is FloatNode, is DoubleNode -> it.push(value.asDouble())
                            is BooleanNode -> it.push(value.asBoolean())
                            is TextNode -> it.push(value.asText())
                            else -> it.pushNil()
                        }
                        return@callable 1
                    }
                    0
                }
            }
        }
    }

    companion object {
        fun getMetadata(element: Any, key: String): Any? {
            return when (element) {
                is MetadataHolder -> {
                    element.metadata[key]
                }

                is JsonNode -> {
                    val value = element["metadata"]?.get(key)
                    when (value) {
                        is LongNode -> value.asLong()
                        is IntNode, is ShortNode -> value.asInt()
                        is FloatNode, is DoubleNode -> value.asDouble()
                        is BooleanNode -> value.asBoolean()
                        is TextNode -> value.asText()
                        else -> null
                    }
                }

                else -> null
            }
        }
    }
}
