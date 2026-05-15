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
import com.seleneworlds.server.script.ServerScriptHotReload

class ServerBundleWatcher(
    logger: Logger,
    bundleDatabase: BundleDatabase,
    private val networkServer: NetworkServer,
    private val registries: Registries,
    private val clientBundleCache: ClientBundleCache,
    private val serverScriptHotReload: ServerScriptHotReload
) : BundleWatcher(logger, bundleDatabase, setOf("common", "client", "server")) {

    override fun processPendingBundleUpdates(
        bundleId: String,
        updatedFiles: Set<String>,
        deletedFiles: Set<String>
    ) {
        super.processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)

        val bundle = bundleDatabase.getBundle(bundleId)
        if (bundle != null) {
            serverScriptHotReload.reloadUpdatedScripts(bundle, updatedFiles)
            serverScriptHotReload.unloadDeletedScripts(bundle, deletedFiles)
        }

        val clientVisibleUpdatedFiles = updatedFiles.filterTo(mutableSetOf()) { isClientVisibleContentPath(it) }
        val clientVisibleDeletedFiles = deletedFiles.filterTo(mutableSetOf()) { isClientVisibleContentPath(it) }

        // Clear client bundle cache when bundle content changes
        if (clientVisibleUpdatedFiles.isNotEmpty() || clientVisibleDeletedFiles.isNotEmpty()) {
            clientBundleCache.clearCacheForBundle(bundleId)
        }

        val packet = NotifyBundleUpdatePacket(
            bundleId = bundleId,
            updated = clientVisibleUpdatedFiles.toList(),
            deleted = clientVisibleDeletedFiles.toList()
        )
        if (packet.updated.isNotEmpty() || packet.deleted.isNotEmpty()) {
            networkServer.clients.forEach { it.send(packet) }
        }
    }

    override fun getRegistry(name: String): Registry<*>? {
        val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(name))
        if (builtinRegistry != null) {
            return builtinRegistry
        }
        return registries.customRegistries.findByRegistryName(name)
    }

    private fun isClientVisibleContentPath(path: String): Boolean {
        return path.startsWith("common/") || path.startsWith("client/")
    }
}
