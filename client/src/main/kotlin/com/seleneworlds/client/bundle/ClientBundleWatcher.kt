package com.seleneworlds.client.bundle

import org.slf4j.Logger
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.data.Registries
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleWatcher
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry

class ClientBundleWatcher(
    logger: Logger,
    bundleDatabase: BundleDatabase,
    private val registries: Registries,
    private val assetProvider: AssetProvider
) : BundleWatcher(logger, bundleDatabase) {

    override fun getRegistry(name: String): Registry<*>? {
        val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(name))
        if (builtinRegistry != null) {
            return builtinRegistry
        }
        return registries.customRegistries.findByRegistryName(name)
    }

    override fun processPendingBundleUpdates(bundleId: String, updatedFiles: Set<String>, deletedFiles: Set<String>) {
        (updatedFiles + deletedFiles)
            .filter { isAssetFile(it) }
            .forEach { assetPath ->
                assetProvider.notifyAssetChanged(assetPath)
            }

        super.processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)
    }

    private fun isAssetFile(filePath: String): Boolean {
        val normalizedFilePath = filePath.replace('\\', '/')
        return assetFilePattern.containsMatchIn(normalizedFilePath)
    }

    companion object {
        private val assetFilePattern = "^(common|client)/assets/[\\w-]+/([\\w-]+)/.*".toRegex()
    }
}
