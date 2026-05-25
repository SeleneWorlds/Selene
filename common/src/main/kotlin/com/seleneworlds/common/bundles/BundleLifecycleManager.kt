package com.seleneworlds.common.bundles

import org.slf4j.Logger

class BundleLifecycleManager(
    private val logger: Logger,
    private val bundleLoader: BundleLoader,
    private val bundleDatabase: BundleDatabase,
    private val runtimeRebuilder: BundleRuntimeRebuilder
) {

    fun initializeBundles(bundles: List<Bundle>) {
        enableBundlesInOrder(bundles)
        rebuildRuntimeAndRunEntrypoints(bundles)
    }

    fun enableBundle(bundle: Bundle) {
        enableBundlesInOrder(listOf(bundle))
        rebuildRuntimeAndRunEntrypoints(listOf(bundle))
    }

    fun disableBundle(bundle: Bundle) {
        disableBundlesInReverseOrder(listOf(bundle))
        runtimeRebuilder.rebuildActiveBundles(bundleDatabase)
    }

    fun reloadBundleClosure(bundleId: String, deletedFiles: Set<String>) {
        val impactedBundleIds = bundleDatabase.getTransitiveDependents(bundleId) + bundleId
        val bundlesToReload = bundleDatabase.enabledBundles.filter { it.manifest.name in impactedBundleIds }
        if (bundlesToReload.isEmpty()) {
            logger.warn("Could not eager reload unknown bundle {}", bundleId)
            return
        }

        disableBundlesInReverseOrder(
            bundlesToReload,
            deletedFilesByBundleName = mapOf(bundleId to deletedFiles)
        )
        enableBundlesInOrder(bundlesToReload)
        rebuildRuntimeAndRunEntrypoints(bundlesToReload)

        logger.info(
            "Eager reloaded bundle closure for {}: {}",
            bundleId,
            bundlesToReload.joinToString(", ") { it.manifest.name }
        )
    }

    private fun enableBundlesInOrder(bundles: List<Bundle>) {
        for (bundle in bundles) {
            bundleDatabase.enableBundle(bundle)
            bundleLoader.preloadBundleModules(bundle)
        }
    }

    private fun disableBundlesInReverseOrder(
        bundles: List<Bundle>,
        deletedFilesByBundleName: Map<String, Set<String>> = emptyMap()
    ) {
        for (bundle in bundles.asReversed()) {
            bundleLoader.clearBundleState(bundle, deletedFilesByBundleName[bundle.manifest.name].orEmpty())
            bundleDatabase.disableBundle(bundle)
        }
    }

    private fun rebuildRuntimeAndRunEntrypoints(bundles: List<Bundle>) {
        runtimeRebuilder.rebuildActiveBundles(bundleDatabase)
        for (bundle in bundles) {
            bundleLoader.runBundleEntrypoints(listOf(bundle), runtimeRebuilder.entrypointFilters)
        }
    }
}
