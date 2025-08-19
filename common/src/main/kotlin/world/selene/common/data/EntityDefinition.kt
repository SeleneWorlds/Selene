package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class EntityDefinition(
    val components: Map<String, ComponentConfiguration> = emptyMap(),
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VisualComponentConfiguration::class, name = "visual"),
    JsonSubTypes.Type(value = ClientScriptComponentConfiguration::class, name = "client_script")
)
interface ComponentConfiguration

data class VisualComponentConfiguration(val visual: String, val properties: Map<String, String> = emptyMap()) : ComponentConfiguration

data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration