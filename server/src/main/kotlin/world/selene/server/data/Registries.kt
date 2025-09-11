package world.selene.server.data

import world.selene.common.data.*
import world.selene.common.data.custom.CustomRegistries
import world.selene.common.entities.EntityRegistry
import world.selene.common.entities.component.ComponentRegistry
import world.selene.common.sounds.SoundRegistry
import world.selene.common.tiles.TileRegistry
import world.selene.common.tiles.transitions.TransitionRegistry

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