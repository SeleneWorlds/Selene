package world.selene.common.data

data class TileDefinition(
    val visual: String,
    val color: String = "#FFFFFF",
    val slowFactor: Int = 0,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val tags: Set<String> = emptySet()
)