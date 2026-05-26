package com.seleneworlds.common.bundles

fun interface BundleStateCleaner {
    fun clearBundleState(bundle: Bundle)

    companion object {
        val Noop = BundleStateCleaner { }
    }
}
