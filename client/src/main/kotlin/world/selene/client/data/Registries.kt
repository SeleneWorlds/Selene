package world.selene.client.data

import world.selene.client.rendering.visual.VisualRegistry
import world.selene.client.sounds.AudioRegistry
import world.selene.common.data.*
import world.selene.common.data.custom.CustomRegistries
import world.selene.common.entities.EntityRegistry
import world.selene.common.sounds.SoundRegistry
import world.selene.common.tiles.TileRegistry

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