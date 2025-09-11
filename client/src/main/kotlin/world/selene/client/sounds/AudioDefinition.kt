package world.selene.client.sounds

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import world.selene.common.data.MetadataHolder
import world.selene.common.data.Registry
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
) : AudioDefinition, MetadataHolder, RegistryObject<AudioDefinition> {
    override var id: Int = 0; private set
    override lateinit var name: String; private set
    override lateinit var registry: Registry<AudioDefinition>; private set

    override fun initializeFromRegistry(
        registry: Registry<AudioDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}

data class MusicAudioDefinition(
    val file: String,
    override val metadata: Map<String, String> = emptyMap()
) : AudioDefinition, MetadataHolder, RegistryObject<AudioDefinition> {
    override var id: Int = 0; private set
    override lateinit var name: String; private set
    override lateinit var registry: Registry<AudioDefinition>; private set

    override fun initializeFromRegistry(
        registry: Registry<AudioDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}