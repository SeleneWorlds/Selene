package world.selene.common.data.custom

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import com.fasterxml.jackson.module.kotlin.treeToValue
import party.iroiro.luajava.Lua
import world.selene.common.util.asAny
import world.selene.common.lua.*
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

class CustomRegistryObject(val registry: CustomRegistry, val name: String, val element: JsonNode) :
    LuaMetatableProvider,
    LuaReferencable<String, CustomRegistryObject> {
    val luaReference = LuaReference(CustomRegistryObject::class, name, registry, this)

    fun getMetadata(key: String): Any? {
        return element["metadata"]?.get(key)?.asAny()
    }

    override fun luaReference(): LuaReference<String, CustomRegistryObject> {
        return luaReference
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "CustomRegistryObject(${registry.name}, $name, $element)"
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Unique name of this custom registry object.
         *
         * ```property
         * Name: string
         * ```
         */
        private fun luaGetName(lua: Lua): Int {
            val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
            lua.push(registryObject.name)
            return 1
        }

        /**
         * Gets metadata value for the specified key from this custom registry object.
         *
         * ```signatures
         * GetMetadata(key: string) -> any|nil
         * ```
         */
        private fun luaGetMetadata(lua: Lua): Int {
            val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
            val key = lua.checkString(2)
            val value = registryObject.getMetadata(key)
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }

        /**
         * Gets a field value from this custom registry object's JSON element.
         *
         * ```signatures
         * GetField(key: string) -> any|nil
         * ```
         */
        private fun luaGetField(lua: Lua): Int {
            val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
            val key = lua.checkString(2)
            val objectMapper = registryObject.registry.objectMapper
            when (val value = registryObject.element[key]) {
                is LongNode -> lua.push(value.asLong())
                is IntNode, is ShortNode -> lua.push(value.asInt())
                is FloatNode, is DoubleNode -> lua.push(value.asDouble())
                is BooleanNode -> lua.push(value.asBoolean())
                is TextNode -> lua.push(value.asText())
                is ArrayNode -> lua.push(objectMapper.treeToValue(value), Lua.Conversion.FULL)
                is ObjectNode -> lua.push(objectMapper.treeToValue(value), Lua.Conversion.FULL)
                else -> lua.pushNil()
            }
            return 1
        }

        val luaMeta = LuaMappedMetatable(CustomRegistryObject::class) {
            getter(::luaGetName)
            callable(::luaGetMetadata)
            callable(::luaGetField)
        }
    }
}