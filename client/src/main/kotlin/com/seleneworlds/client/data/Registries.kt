package com.seleneworlds.client.data

import com.seleneworlds.client.grid.RenderGridRegistry
import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.client.sounds.AudioRegistry
import com.seleneworlds.client.particles.ParticleSystemRegistry
import com.seleneworlds.client.timeline.TimelineRegistry
import com.seleneworlds.common.data.*
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry

class Registries(
    val tiles: TileRegistry,
    val entities: EntityRegistry,
    val grids: GridRegistry,
    val renderGrids: RenderGridRegistry,
    val visuals: VisualRegistry,
    val sounds: SoundRegistry,
    val audios: AudioRegistry,
    val particles: ParticleSystemRegistry,
    val timelines: TimelineRegistry,
    val customRegistries: CustomRegistries
) : RegistryProvider {

    override fun getRegistries(): Collection<Registry<*>> =
        listOf(tiles, entities, grids, renderGrids, visuals, sounds, audios, particles, timelines, customRegistries) + customRegistries.getAllCustomRegistries()

    override fun getRegistry(identifier: Identifier): Registry<*>? {
        return when (identifier) {
            TileRegistry.IDENTIFIER -> tiles
            EntityRegistry.IDENTIFIER -> entities
            GridRegistry.IDENTIFIER -> grids
            RenderGridRegistry.IDENTIFIER -> renderGrids
            VisualRegistry.IDENTIFIER -> visuals
            SoundRegistry.IDENTIFIER -> sounds
            AudioRegistry.IDENTIFIER -> audios
            ParticleSystemRegistry.IDENTIFIER -> particles
            TimelineRegistry.IDENTIFIER -> timelines
            CustomRegistries.IDENTIFIER -> customRegistries
            else -> customRegistries.getCustomRegistry(identifier)
        }
    }
}
