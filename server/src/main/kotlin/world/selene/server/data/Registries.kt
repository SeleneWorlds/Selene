package world.selene.server.data

import world.selene.common.data.ComponentRegistry
import world.selene.common.data.EntityRegistry
import world.selene.common.data.NameIdRegistry
import world.selene.common.data.Registry
import world.selene.common.data.RegistryProvider
import world.selene.common.data.CustomRegistries
import world.selene.common.data.SoundRegistry
import world.selene.common.data.TileRegistry
import world.selene.common.data.TransitionRegistry

class Registries(
    val mappings: NameIdRegistry,
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