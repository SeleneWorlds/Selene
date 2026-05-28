package com.seleneworlds.common.bundles

interface BundleLifecycleOperations {
    fun preloadBundleModules(bundle: Bundle)

    fun clearBundleState(bundle: Bundle, deletedFiles: Set<String> = emptySet())

    fun runBundleEntrypoints(bundles: List<Bundle>, entrypointFilters: List<String>)
}
