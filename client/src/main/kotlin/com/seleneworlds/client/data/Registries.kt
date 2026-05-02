package com.seleneworlds.client.data

import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.client.sounds.AudioRegistry
import com.seleneworlds.common.data.*
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry

class Registries(
    val tiles: TileRegistry,
    val entities: EntityRegistry,
    val visuals: VisualRegistry,
    val sounds: SoundRegistry,
    val audios: AudioRegistry,
    val customRegistries: CustomRegistries
) : RegistryProvider {

    override fun getRegistry(identifier: Identifier): Registry<*>? {
        return when (identifier) {
            TileRegistry.IDENTIFIER -> tiles
            EntityRegistry.IDENTIFIER -> entities
            VisualRegistry.IDENTIFIER -> visuals
            SoundRegistry.IDENTIFIER -> sounds
            AudioRegistry.IDENTIFIER -> audios
            CustomRegistries.IDENTIFIER -> customRegistries
            else -> customRegistries.getCustomRegistry(identifier)
        }
    }
}