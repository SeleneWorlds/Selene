package world.selene.server.data

import world.selene.common.data.*

class Registries(
    val tiles: TileRegistry,
    val transitions: TransitionRegistry,
    val entities: EntityRegistry,
    val components: ComponentRegistry,
    val sounds: SoundRegistry,
    val customRegistries: CustomRegistries
) : RegistryProvider {

    override fun getRegistry(name: String): Registry<*>? {
        return when (name) {
            "tiles" -> tiles
            "transitions" -> transitions
            "entities" -> entities
            "components" -> components
            "sounds" -> sounds
            "registries" -> customRegistries
            else -> customRegistries.getCustomRegistry(name)
        }
    }
}