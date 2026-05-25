package com.seleneworlds.common.bundles

interface BundleRuntimeRebuilder {
    val entrypointFilters: List<String>

    fun rebuildActiveBundles(bundleDatabase: BundleDatabase)
}
