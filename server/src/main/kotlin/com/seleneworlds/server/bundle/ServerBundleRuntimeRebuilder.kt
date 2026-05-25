package com.seleneworlds.server.bundle

import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleRuntimeRebuilder
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.ActiveGrid
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.data.mappings.PersistentNameIdRegistry

class ServerBundleRuntimeRebuilder(
    private val tileRegistry: TileRegistry,
    private val transitionRegistry: TransitionRegistry,
    private val componentRegistry: ComponentRegistry,
    private val soundRegistry: SoundRegistry,
    private val entityRegistry: EntityRegistry,
    private val gridRegistry: GridRegistry,
    private val customRegistries: CustomRegistries,
    private val activeGrid: ActiveGrid,
    private val nameIdRegistry: PersistentNameIdRegistry,
    private val config: ServerConfig,
    private val logger: Logger
) : BundleRuntimeRebuilder {

    override val entrypointFilters: List<String> = listOf("common/", "server/", "init.lua")

    override fun rebuildActiveBundles(bundleDatabase: BundleDatabase) {
        logger.info("Rebuilding server runtime from active bundles")

        tileRegistry.load(bundleDatabase)
        transitionRegistry.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        soundRegistry.load(bundleDatabase)
        entityRegistry.load(bundleDatabase)
        gridRegistry.load(bundleDatabase)
        activeGrid.applyGrid(config.grid)
        customRegistries.load(bundleDatabase)
        customRegistries.loadCustomRegistries(bundleDatabase, "common")
        customRegistries.loadCustomRegistries(bundleDatabase, "server")

        nameIdRegistry.watch(tileRegistry)
        nameIdRegistry.watch(componentRegistry)
        nameIdRegistry.watch(entityRegistry)
        nameIdRegistry.watch(soundRegistry)
        nameIdRegistry.load()
        nameIdRegistry.populate(tileRegistry)
        nameIdRegistry.populate(componentRegistry)
        nameIdRegistry.populate(entityRegistry)
        nameIdRegistry.populate(soundRegistry)
        nameIdRegistry.save()
    }
}
