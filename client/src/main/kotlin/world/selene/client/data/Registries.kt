package world.selene.client.data

import world.selene.common.data.EntityRegistry
import world.selene.common.data.Registry
import world.selene.common.data.RegistryProvider
import world.selene.common.data.CustomRegistries
import world.selene.common.data.SoundRegistry
import world.selene.common.data.TileRegistry

class Registries(
    val tiles: TileRegistry,
    val entities: EntityRegistry,
    val visuals: VisualRegistry,
    val sounds: SoundRegistry,
    val audios: AudioRegistry,
    val customRegistries: CustomRegistries
) : RegistryProvider {
    
    override fun getRegistry(name: String): Registry<*>? {
        return when (name) {
            "tiles" -> tiles
            "entities" -> entities
            "visuals" -> visuals
            "sounds" -> sounds
            "audios" -> audios
            "registries" -> customRegistries
            else -> customRegistries.getCustomRegistry(name)
        }
    }
}