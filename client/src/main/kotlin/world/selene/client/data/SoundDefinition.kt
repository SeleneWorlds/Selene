package world.selene.client.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SimpleSoundDefinition::class, name = "simple")
)
interface SoundDefinition

data class SimpleSoundDefinition(
    val file: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val loop: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) : SoundDefinition