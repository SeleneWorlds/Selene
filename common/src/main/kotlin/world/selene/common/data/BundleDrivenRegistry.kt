package world.selene.common.data

import world.selene.common.bundles.Bundle
import world.selene.common.bundles.BundleDatabase

interface BundleDrivenRegistry {
    fun load(bundleDatabase: BundleDatabase)

    fun bundleFileUpdated(bundleDatabase: BundleDatabase, bundle: Bundle, path: String)

    fun bundleFileRemoved(bundleDatabase: BundleDatabase, bundle: Bundle, path: String)
}