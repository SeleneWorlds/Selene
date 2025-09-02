package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class EntityDefinition(
    val components: Map<String, ComponentConfiguration> = emptyMap(),
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryObject<EntityDefinition> {
    override var id: Int = 0; private set
    override lateinit var name: String; private set
    override lateinit var registry: Registry<EntityDefinition>; private set

    override fun initializeFromRegistry(
        registry: Registry<EntityDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VisualComponentConfiguration::class, name = "visual"),
    JsonSubTypes.Type(value = ClientScriptComponentConfiguration::class, name = "client_script")
)
interface ComponentConfiguration

data class VisualComponentConfiguration(val visual: String, val overrides: Map<String, Any> = emptyMap()) : ComponentConfiguration

data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration