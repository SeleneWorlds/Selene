package com.seleneworlds.common.data

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase

interface BundleDrivenRegistry {
    fun load(bundleDatabase: BundleDatabase)

    fun bundleFileUpdated(bundleDatabase: BundleDatabase, bundle: Bundle, path: String)

    fun bundleFileRemoved(bundleDatabase: BundleDatabase, bundle: Bundle, path: String)
}