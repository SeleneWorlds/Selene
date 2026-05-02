package com.seleneworlds.common.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.data.TagHolder
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
data class EntityDefinition(
    val components: Map<String, ComponentConfiguration> = emptyMap(),
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<EntityDefinition>()

@Serializable
sealed interface ComponentConfiguration

@Serializable
data class VisualComponentPosition(val origin: String = "none", val offsetX: Float = 0f, val offsetY: Float = 0f) {
    companion object {
        val Default = VisualComponentPosition()
    }
}

@Serializable
@SerialName("visual")
data class VisualComponentConfiguration(
    val visual: Identifier,
    val position: VisualComponentPosition = VisualComponentPosition.Default,
    @Serializable(with = SerializedMapSerializer::class)
    val overrides: SerializedMap = emptyMap()
) : ComponentConfiguration

@Serializable
@SerialName("client_script")
data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration
