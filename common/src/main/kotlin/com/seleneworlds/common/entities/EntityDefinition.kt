package com.seleneworlds.common.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.data.TagHolder

data class EntityDefinition(
    val components: Map<String, ComponentConfiguration> = emptyMap(),
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<EntityDefinition>()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VisualComponentConfiguration::class, name = "visual"),
    JsonSubTypes.Type(value = ClientScriptComponentConfiguration::class, name = "client_script")
)
interface ComponentConfiguration

data class VisualComponentPosition(val origin: String = "none", val offsetX: Float = 0f, val offsetY: Float = 0f) {
    companion object {
        val Default = VisualComponentPosition()
    }
}

data class VisualComponentConfiguration(
    val visual: Identifier,
    val position: VisualComponentPosition = VisualComponentPosition.Default,
    val overrides: Map<String, Any> = emptyMap()
) : ComponentConfiguration

data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration