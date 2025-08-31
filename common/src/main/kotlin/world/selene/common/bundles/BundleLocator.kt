package world.selene.common.bundles

import party.iroiro.luajava.value.LuaValue
import java.io.File

class LocatedBundle(val manifest: BundleManifest, val dir: File) {
    val transformers = mutableMapOf<String, LuaValue>()

    override fun toString(): String {
        return manifest.name + " (" + dir.absolutePath + ")"
    }

    fun getFileDebugName(file: File): String {
        val sb = StringBuilder()
        sb.append(manifest.name.replace(Regex("[aiueoAIUEO]"), ""))
        sb.append(":")
        if (file.parentFile.name != "lua") {
            sb.append(file.parentFile.name).append("/")
        }
        sb.append(file.name)
        return sb.toString()
    }
}

interface BundleLocator {
    fun locateBundle(name: String): LocatedBundle?
}

