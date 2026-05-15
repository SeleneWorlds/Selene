package com.seleneworlds.client

import org.slf4j.Logger
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.data.Registries
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.ActiveGrid

class ClientReloadManager(
    private val bundleDatabase: BundleDatabase,
    private val registries: Registries,
    private val componentRegistry: ComponentRegistry,
    private val assetProvider: AssetProvider,
    private val activeGrid: ActiveGrid,
    private val nameIdRegistry: NameIdRegistry,
    private val logger: Logger
) {

    fun reloadRegistriesAndTextures() {
        logger.info("Reloading client registries and textures")

        registries.tiles.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        registries.sounds.load(bundleDatabase)
        registries.entities.load(bundleDatabase)
        registries.grids.load(bundleDatabase)
        registries.visuals.load(bundleDatabase)
        registries.audios.load(bundleDatabase)
        registries.customRegistries.load(bundleDatabase)
        activeGrid.reapply()
        reloadCustomRegistries("common")
        reloadCustomRegistries("client")
        repopulateNameIdMappings()

        assetProvider.reloadSubscribedTextures()
    }

    private fun reloadCustomRegistries(platform: String) {
        registries.customRegistries.loadCustomRegistries(bundleDatabase, platform)
    }

    private fun repopulateNameIdMappings() {
        listOf<Registry<*>>(
            registries.tiles,
            componentRegistry,
            registries.sounds,
            registries.entities,
            registries.visuals,
            registries.audios,
            registries.customRegistries
        ).forEach {
            it.registryPopulated(nameIdRegistry, false)
        }

        registries.customRegistries.getAllCustomRegistries().forEach {
            it.registryPopulated(nameIdRegistry, false)
        }
    }
}
