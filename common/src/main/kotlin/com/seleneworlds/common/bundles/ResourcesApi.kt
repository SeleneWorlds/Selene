package com.seleneworlds.common.bundles

import com.seleneworlds.common.data.json.FileBasedRegistry
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class ResourcesApi(private val bundleDatabase: BundleDatabase) {

    fun listBundles(): List<String> = bundleDatabase.enabledBundles.map { it.manifest.name }

    /** List individual JSON entry files across namespaces for this registry in a bundle. */
    fun listRegistryFiles(bundle: String, registry: FileBasedRegistry<*>): List<String> {
        val baseDir = bundleDatabase.getBundle(bundle)?.dir ?: return emptyList()
        val dataDir = baseDir.resolve("${registry.platform}/data")
        return dataDir.listFiles().orEmpty().asSequence()
            .filter { it.isDirectory }
            .flatMap { namespace -> namespace.resolve(registry.name).walkTopDown().asSequence() }
            .filter { it.isFile && it.name.endsWith(".json") }
            .map { "$bundle/${it.relativeTo(baseDir).invariantSeparatorsPath}" }
            .filter { registry.getResourceIdentifier(bundle, it) != null }
            .toList()
    }

    /** List files matching a glob relative to the bundle root; wildcards also match nested paths. */
    fun listFiles(bundle: String, filter: String): List<String> {
        val baseDir = bundleDatabase.getBundle(bundle)?.dir ?: return emptyList()
        val matcher = globToRegex(filter)
        return bundleContentFiles(baseDir).filter {
            matcher.matches(it.relativeTo(baseDir).invariantSeparatorsPath)
        }.map {
            bundle + File.separator + it.relativeTo(baseDir).path
        }.toList()
    }

    fun loadAsString(path: String): String {
        val (_, file) = resolveFile(path)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File not found: $path")
        }
        return file.readText()
    }

    fun saveAsString(path: String, contents: String) {
        val (_, file) = resolveFile(path)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File not found: $path")
        }
        file.writeText(contents)
    }

    fun fileExists(path: String): Boolean {
        return runCatching {
            val (_, file) = resolveFile(path)
            file.exists() && file.isFile
        }.getOrDefault(false)
    }

    fun createAsString(path: String, contents: String, createDirectories: Boolean = false) {
        val (_, file) = resolveFile(path)
        if (createDirectories) Files.createDirectories(file.parentFile.toPath())
        require(file.parentFile.isDirectory) { "Resource directory not found: $path" }
        Files.writeString(file.toPath(), contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    }

    fun removeFile(path: String) {
        val (_, file) = resolveFile(path)
        require(file.isFile) { "File not found: $path" }
        Files.delete(file.toPath())
    }

    private fun resolveFile(path: String): Pair<File, File> {
        val bundleName = path.substringBefore("/")
        val remainingPath = path.substringAfter("/", "")
        require(bundleName.isNotEmpty() && remainingPath.isNotEmpty()) { "Invalid resource path: $path" }
        val baseDir = bundleDatabase.getBundle(bundleName)?.dir?.canonicalFile
            ?: throw IllegalArgumentException("Failed to find bundle: $bundleName")
        val file = resolveBundleContentFile(baseDir, remainingPath)
            ?: throw IllegalArgumentException("Invalid file path: $path")
        return baseDir to file
    }

    private fun globToRegex(glob: String): Regex {
        return glob
            .replace("\\", "\\\\")
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex()
    }
}
