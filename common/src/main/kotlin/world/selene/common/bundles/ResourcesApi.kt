package world.selene.common.bundles

import java.io.File

class ResourcesApi(private val bundleDatabase: BundleDatabase) {

    fun listFiles(bundle: String, filter: String): List<String> {
        val baseDir = bundleDatabase.getBundle(bundle)?.dir ?: return emptyList()
        return baseDir.walkTopDown().filter {
            it.isFile && it.relativeTo(baseDir).path.matches(globToRegex(filter))
        }.map {
            bundle + File.separator + it.relativeTo(baseDir).path
        }.toList()
    }

    fun loadAsString(path: String): String {
        val bundleName = path.substringBefore("/")
        val remainingPath = path.substringAfter("/")
        val baseDir = bundleDatabase.getBundle(bundleName)?.dir
            ?: throw IllegalArgumentException("Failed to find bundle: $bundleName")

        val file = baseDir.resolve(remainingPath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File not found: $path")
        }
        if (!file.path.startsWith(baseDir.path)) {
            throw IllegalArgumentException("Invalid file path: $path")
        }
        return file.readText()
    }

    fun fileExists(path: String): Boolean {
        val bundleName = path.substringBefore("/")
        val remainingPath = path.substringAfter("/")
        val baseDir = bundleDatabase.getBundle(bundleName)?.dir ?: return false
        val file = baseDir.resolve(remainingPath)
        return file.exists() && file.isFile && file.path.startsWith(baseDir.path)
    }

    private fun globToRegex(glob: String): Regex {
        return glob
            .replace("\\", "\\\\")
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex()
    }
}
