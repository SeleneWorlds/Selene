package world.selene.common.data

data class TileDefinition(
    val visual: String,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryObject<TileDefinition> {
    override var id: Int = 0; private set
    override lateinit var name: String; private set
    override lateinit var registry: Registry<TileDefinition>; private set

    override fun initializeFromRegistry(
        registry: Registry<TileDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}