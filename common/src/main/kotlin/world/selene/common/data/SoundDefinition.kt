package world.selene.common.data

data class SoundDefinition(
    val audio: String,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryObject<SoundDefinition> {
    override var id: Int = 0; private set
    override lateinit var name: String; private set
    override lateinit var registry: Registry<SoundDefinition>; private set

    override fun initializeFromRegistry(
        registry: Registry<SoundDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}