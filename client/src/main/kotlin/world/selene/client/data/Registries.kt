package world.selene.client.data

import world.selene.common.data.*

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