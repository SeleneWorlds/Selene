package com.seleneworlds.server.bundle

import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleWatcher
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.network.packet.NotifyBundleUpdatePacket
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.network.NetworkServer

class ServerBundleWatcher(
    logger: Logger,
    bundleDatabase: BundleDatabase,
    private val networkServer: NetworkServer,
    private val registries: Registries,
    private val clientBundleCache: ClientBundleCache
) : BundleWatcher(logger, bundleDatabase) {

    override fun processPendingBundleUpdates(
        bundleId: String,
        updatedFiles: Set<String>,
        deletedFiles: Set<String>
    ) {
        super.processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)

        // Clear client bundle cache when bundle content changes
        if (updatedFiles.isNotEmpty() || deletedFiles.isNotEmpty()) {
            clientBundleCache.clearCacheForBundle(bundleId)
        }

        val packet = NotifyBundleUpdatePacket(
            bundleId = bundleId,
            updated = updatedFiles.toList(),
            deleted = deletedFiles.toList()
        )
        networkServer.clients.forEach { it.send(packet) }
    }

    override fun getRegistry(name: String): Registry<*>? {
        val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(name))
        if (builtinRegistry != null) {
            return builtinRegistry
        }
        return registries.customRegistries.findByRegistryName(name)
    }

}
