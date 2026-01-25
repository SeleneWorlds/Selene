package world.selene.common.tiles

import world.selene.common.data.Identifier
import world.selene.common.data.MetadataHolder
import world.selene.common.data.RegistryAdoptedObject
import world.selene.common.data.TagHolder

data class TileDefinition(
    val visual: Identifier,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<TileDefinition>()