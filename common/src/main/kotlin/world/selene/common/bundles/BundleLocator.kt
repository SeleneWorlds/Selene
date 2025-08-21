package world.selene.common.bundles

import party.iroiro.luajava.value.LuaValue
import java.io.File

class LocatedBundle(val manifest: BundleManifest, val dir: File) {
    val transformers = mutableMapOf<String, LuaValue>()

    override fun toString(): String {
        return manifest.name + " (" + dir.absolutePath + ")"
    }
}

interface BundleLocator {
    fun locateBundle(name: String): LocatedBundle?
}

