package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString

data class EntityDefinition(
    val components: Map<String, ComponentConfiguration> = emptyMap(),
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, LuaMetatableProvider {
    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(EntityDefinition::class) {
            callable("GetMetadata") {
                val definition = it.checkSelf()
                val key = it.checkString(2)
                val value = definition.metadata[key]
                if (value != null) {
                    it.push(value, Lua.Conversion.FULL)
                    return@callable 1
                }
                0
            }
            callable("HasTag") {
                val definition = it.checkSelf()
                val key = it.checkString(2)
                it.push(definition.tags.contains(key))
                1
            }
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VisualComponentConfiguration::class, name = "visual"),
    JsonSubTypes.Type(value = ClientScriptComponentConfiguration::class, name = "client_script")
)
interface ComponentConfiguration

data class VisualComponentConfiguration(val visual: String, val properties: Map<String, String> = emptyMap()) : ComponentConfiguration

data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration