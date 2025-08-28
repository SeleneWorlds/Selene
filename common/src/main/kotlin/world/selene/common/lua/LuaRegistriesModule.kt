package world.selene.common.lua

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.treeToValue
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
        val value = lua.toAny(3)
            ?: return lua.error(IllegalArgumentException("Value must not be nil"))

        val registry = registryProvider.getRegistry(registryName)
            ?: return lua.error(IllegalArgumentException("Unknown registry: $registryName"))

        val found = registry.findByMetadata(key, value)
        if (found != null) {
            lua.push(TransientRegistryObject(found.first, found.second), Lua.Conversion.NONE)
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
        lua.push(TransientRegistryObject(name, element), Lua.Conversion.NONE)
        return 1
    }

    class TransientRegistryObject(val name: String, val element: Any) : LuaMetatableProvider {
        override fun luaMetatable(lua: Lua): LuaMetatable {
            return luaMeta
        }

        override fun toString(): String {
            return "TransientRegistryObject($name, $element)"
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
                callable("GetField") { lua ->
                    val registryObject = lua.checkSelf()
                    val key = lua.checkString(2)
                    if (registryObject.element is JsonNode) {
                        when (val value = registryObject.element[key]) {
                            is LongNode -> lua.push(value.asLong())
                            is IntNode, is ShortNode -> lua.push(value.asInt())
                            is FloatNode, is DoubleNode -> lua.push(value.asDouble())
                            is BooleanNode -> lua.push(value.asBoolean())
                            is TextNode -> lua.push(value.asText())
                            is ArrayNode -> lua.push(ObjectMapper().treeToValue(value), Lua.Conversion.FULL)
                            is ObjectNode -> lua.push(ObjectMapper().treeToValue(value), Lua.Conversion.FULL)
                            else -> lua.pushNil()
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
                    when (val value = element["metadata"]?.get(key)) {
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
