package world.selene.common.bundles

class BundleDatabase {
    // All loaded bundles, with preserved order
    val loadedBundles = mutableListOf<LocatedBundle>()

    fun addBundle(bundle: LocatedBundle) {
        loadedBundles.add(bundle)
    }

    fun getBundle(bundle: String): LocatedBundle? {
        return loadedBundles.find { it.manifest.name == bundle }
    }
}