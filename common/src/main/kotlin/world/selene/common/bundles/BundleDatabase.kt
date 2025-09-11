package world.selene.common.bundles

class BundleDatabase {
    // All loaded bundles, with preserved order
    val loadedBundles = mutableListOf<Bundle>()

    fun addBundle(bundle: Bundle) {
        loadedBundles.add(bundle)
    }

    fun getBundle(bundle: String): Bundle? {
        return loadedBundles.find { it.manifest.name == bundle }
    }
}