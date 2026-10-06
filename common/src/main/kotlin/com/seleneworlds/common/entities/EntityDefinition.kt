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
@SerialName("impassable")
data class ImpassableComponentConfiguration(
    val enabled: Boolean = true
) : ComponentConfiguration

@Serializable
@SerialName("passable_above")
data class PassableAboveComponentConfiguration(
    val enabled: Boolean = true
) : ComponentConfiguration

@Serializable
@SerialName("draggable")
data class DraggableComponentConfiguration(
    val enabled: Boolean = true
) : ComponentConfiguration

@Serializable
@SerialName("ignores_elevation")
class IgnoresElevationComponentConfiguration : ComponentConfiguration

@Serializable
@SerialName("gravity")
class GravityComponentConfiguration : ComponentConfiguration

@Serializable
@SerialName("light")
data class LightComponentConfiguration(
    val radius: Float,
    val intensity: Float = 1f,
    val red: Float = 1f,
    val green: Float = 1f,
    val blue: Float = 1f,
) : ComponentConfiguration

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
    val red: Float = 1f,
    val green: Float = 1f,
    val blue: Float = 1f,
    val alpha: Float = 1f,
    @Serializable(with = SerializedMapSerializer::class)
    val overrides: SerializedMap = emptyMap()
) : ComponentConfiguration

@Serializable
@SerialName("client_script")
data class ClientScriptComponentConfiguration(val script: String) : ComponentConfiguration

@Serializable
@SerialName("server_script")
data class ServerScriptComponentConfiguration(val script: String) : ComponentConfiguration
