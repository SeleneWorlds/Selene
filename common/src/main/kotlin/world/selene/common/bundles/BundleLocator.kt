package world.selene.common.bundles

import java.io.File

class LocatedBundle(val manifest: BundleManifest, val dir: File)

interface BundleLocator {
    fun locateBundle(name: String): LocatedBundle?
}

