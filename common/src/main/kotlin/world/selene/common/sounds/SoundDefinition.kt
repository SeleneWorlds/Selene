package world.selene.common.sounds

import world.selene.common.data.MetadataHolder
import world.selene.common.data.RegistryAdoptedObject
import world.selene.common.data.TagHolder

data class SoundDefinition(
    val audio: String,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<SoundDefinition>()