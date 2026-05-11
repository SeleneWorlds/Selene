package com.seleneworlds.client

import org.slf4j.Logger
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.data.Registries
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.entities.component.ComponentRegistry

class ClientReloadManager(
    private val bundleDatabase: BundleDatabase,
    private val registries: Registries,
    private val componentRegistry: ComponentRegistry,
    private val assetProvider: AssetProvider,
    private val logger: Logger
) {

    fun reloadRegistriesAndTextures() {
        logger.info("Reloading client registries and textures")

        registries.tiles.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        registries.sounds.load(bundleDatabase)
        registries.entities.load(bundleDatabase)
        registries.visuals.load(bundleDatabase)
        registries.audios.load(bundleDatabase)
        registries.customRegistries.load(bundleDatabase)
        reloadCustomRegistries("common")
        reloadCustomRegistries("client")

        assetProvider.reloadSubscribedTextures()
    }

    private fun reloadCustomRegistries(platform: String) {
        registries.customRegistries.loadCustomRegistries(bundleDatabase, platform)
    }
}
