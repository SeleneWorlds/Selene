package com.seleneworlds.server.data

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry

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