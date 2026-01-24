package world.selene.server.bundle

import org.slf4j.Logger
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleWatcher
import world.selene.common.data.Identifier
import world.selene.common.data.Registry
import world.selene.common.network.packet.NotifyBundleUpdatePacket
import world.selene.server.bundles.ClientBundleCache
import world.selene.server.data.Registries
import world.selene.server.network.NetworkServer

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
