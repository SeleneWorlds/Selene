package com.seleneworlds.client.bundle

import org.slf4j.Logger
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.config.ClientConfig
import com.seleneworlds.client.data.Registries
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.ScriptHotReload
import com.seleneworlds.common.bundles.BundleWatcher
import com.seleneworlds.common.config.HotReloadMode
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.client.ui.cef.CefBrowserUi

class ClientBundleWatcher(
    logger: Logger,
    bundleDatabase: BundleDatabase,
    private val registries: Registries,
    private val assetProvider: AssetProvider,
    private val scriptHotReload: ScriptHotReload,
    private val config: ClientConfig,
    private val browserUi: CefBrowserUi
) : BundleWatcher(logger, bundleDatabase) {

    override fun getRegistry(name: String): Registry<*>? {
        val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(name))
        if (builtinRegistry != null) {
            return builtinRegistry
        }
        return registries.customRegistries.findByRegistryName(name)
    }

    override fun processPendingBundleUpdates(bundleId: String, updatedFiles: Set<String>, deletedFiles: Set<String>) {
        val changedFiles = updatedFiles + deletedFiles
        val bundle = bundleDatabase.getBundle(bundleId)
        if (bundle?.manifest?.ui?.isNotEmpty() == true && changedFiles.any(::isBrowserUiFile)) {
            browserUi.requestReload()
        }
        changedFiles
            .filter { isAssetFile(it) }
            .forEach { assetPath ->
                assetProvider.notifyAssetChanged(assetPath)
            }

        if (bundle != null) {
            if (config.hotReloadMode == HotReloadMode.EAGER) {
                scriptHotReload.reloadBundleClosure(bundleId, deletedFiles)
            } else {
                super.processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)
                scriptHotReload.reloadUpdatedScripts(bundle, updatedFiles)
                scriptHotReload.unloadDeletedScripts(bundle, deletedFiles)
            }
            return
        }

        super.processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)
    }

    private fun isAssetFile(filePath: String): Boolean {
        val normalizedFilePath = filePath.replace('\\', '/')
        return assetFilePattern.containsMatchIn(normalizedFilePath)
    }

    companion object {
        private val assetFilePattern = "^(common|client)/assets/[\\w-]+/([\\w-]+)/.*".toRegex()

        internal fun isBrowserUiFile(filePath: String): Boolean {
            val normalized = filePath.replace('\\', '/')
            return normalized == "bundle.json" || normalized.startsWith("client/")
        }
    }
}
