package world.selene.server.data

import world.selene.common.data.Identifier
import world.selene.common.data.Registry
import world.selene.common.data.RegistryProvider
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

    override fun getRegistry(identifier: Identifier): Registry<*>? {
        return when (identifier) {
            TileRegistry.IDENTIFIER -> tiles
            TransitionRegistry.IDENTIFIER -> transitions
            EntityRegistry.IDENTIFIER -> entities
            ComponentRegistry.IDENTIFIER -> components
            SoundRegistry.IDENTIFIER -> sounds
            CustomRegistries.IDENTIFIER -> customRegistries
            else -> customRegistries.getCustomRegistry(identifier)
        }
    }
}