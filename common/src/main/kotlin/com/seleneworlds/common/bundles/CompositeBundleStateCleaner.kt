package com.seleneworlds.common.bundles

class CompositeBundleStateCleaner(
    private val cleaners: List<BundleStateCleaner>
) : BundleStateCleaner {
    override fun clearBundleState(bundle: Bundle) {
        cleaners.forEach { it.clearBundleState(bundle) }
    }
}
