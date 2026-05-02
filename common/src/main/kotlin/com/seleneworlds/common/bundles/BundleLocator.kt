package com.seleneworlds.common.bundles

interface BundleLocator {
    fun locateBundle(name: String): Bundle?
}

