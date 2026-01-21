package world.selene.client.sounds

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import world.selene.common.data.MetadataHolder
import world.selene.common.data.RegistryAdoptedObject
import world.selene.common.data.RegistryObject

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SimpleAudioDefinition::class, name = "simple"),
    JsonSubTypes.Type(value = MusicAudioDefinition::class, name = "music")
)
interface AudioDefinition

data class SimpleAudioDefinition(
    val file: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val loop: Boolean = false,
    override val metadata: Map<String, String> = emptyMap()
) : AudioDefinition, MetadataHolder, RegistryAdoptedObject<AudioDefinition>()

data class MusicAudioDefinition(
    val file: String,
    override val metadata: Map<String, String> = emptyMap()
) : AudioDefinition, MetadataHolder, RegistryAdoptedObject<AudioDefinition>()