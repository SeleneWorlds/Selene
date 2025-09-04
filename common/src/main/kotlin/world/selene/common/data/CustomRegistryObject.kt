package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
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
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaReferencable
import world.selene.common.lua.LuaReference
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

class CustomRegistryObject(val registry: CustomRegistry, val name: String, val element: JsonNode) : LuaMetatableProvider,
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

    companion object {
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
            readOnly(CustomRegistryObject::name)
            callable(::luaGetMetadata)
            callable(::luaGetField)
        }
    }
}