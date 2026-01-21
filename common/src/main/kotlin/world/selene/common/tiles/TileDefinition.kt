package world.selene.common.tiles

import world.selene.common.data.MetadataHolder
import world.selene.common.data.RegistryObject
import world.selene.common.data.TagHolder

data class TileDefinition(
    val visual: String,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryObject<TileDefinition>()