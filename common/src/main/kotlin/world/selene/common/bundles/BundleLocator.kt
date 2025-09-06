package world.selene.common.bundles

import java.io.File

class LocatedBundle(val manifest: BundleManifest, val dir: File) {
    override fun toString(): String {
        return manifest.name + " (" + dir.absolutePath + ")"
    }

    private val stopPaths = setOf("server", "common", "client", "lua")
    fun getFileDebugName(file: File): String {
        val sb = StringBuilder()
        sb.append(manifest.name)
        sb.append(":")

        val pathParts = mutableListOf<String>()
        var currentDir = file.parentFile
        while (currentDir != null && currentDir.name !in stopPaths) {
            pathParts.add(0, currentDir.name)
            currentDir = currentDir.parentFile
        }

        for (part in pathParts) {
            sb.append(part).append("/")
        }

        sb.append(file.name)
        var result = sb.toString()
        if (result.length >= 45) {
            result = result.removeSuffix(".lua")
        }
        if (result.length >= 45) {
            result = result.replaceFirst(manifest.name, manifest.name.replace(Regex("[AIUEOaiueo]"), ""))
        }
        return result
    }
}

interface BundleLocator {
    fun locateBundle(name: String): LocatedBundle?
}

