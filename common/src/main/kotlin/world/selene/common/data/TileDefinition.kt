package world.selene.common.data

data class TileDefinition(
    val visual: String,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
): MetadataHolder, TagHolder