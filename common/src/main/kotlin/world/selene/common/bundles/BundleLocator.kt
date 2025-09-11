package world.selene.common.bundles

interface BundleLocator {
    fun locateBundle(name: String): Bundle?
}

