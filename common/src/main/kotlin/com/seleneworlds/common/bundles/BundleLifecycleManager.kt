package com.seleneworlds.common.bundles

import org.slf4j.Logger

class BundleLifecycleManager(
    private val logger: Logger,
    private val bundleLoader: BundleLifecycleOperations,
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

        reloadBundles(bundlesToReload, deletedFilesByBundleName = mapOf(bundleId to deletedFiles))

        logger.info(
            "Eager reloaded bundle closure for {}: {}",
            bundleId,
            bundlesToReload.joinToString(", ") { it.manifest.name }
        )
    }

    fun reloadActiveBundles() {
        val bundlesToReload = bundleDatabase.enabledBundles
        if (bundlesToReload.isEmpty()) {
            logger.warn("Could not reload active bundles because no bundles are enabled")
            return
        }

        reloadBundles(bundlesToReload)

        logger.info(
            "Reloaded active bundle runtime: {}",
            bundlesToReload.joinToString(", ") { it.manifest.name }
        )
    }

    private fun reloadBundles(
        bundles: List<Bundle>,
        deletedFilesByBundleName: Map<String, Set<String>> = emptyMap()
    ) {
        disableBundlesInReverseOrder(bundles, deletedFilesByBundleName)
        enableBundlesInOrder(bundles)
        rebuildRuntimeAndRunEntrypoints(bundles)
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
        runtimeRebuilder.onRuntimeRebuilt()
    }
}
