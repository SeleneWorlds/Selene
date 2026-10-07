package com.seleneworlds.common.bundles

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class ResourcesApi(private val bundleDatabase: BundleDatabase) {

    fun listBundles(): List<String> = bundleDatabase.enabledBundles.map { it.manifest.name }

    fun listFiles(bundle: String, filter: String): List<String> {
        val baseDir = bundleDatabase.getBundle(bundle)?.dir ?: return emptyList()
        return baseDir.walkTopDown().filter {
            it.isFile && it.relativeTo(baseDir).path.matches(globToRegex(filter))
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

    fun createAsString(path: String, contents: String) {
        val (_, file) = resolveFile(path)
        require(file.parentFile.isDirectory) { "Resource directory not found: $path" }
        Files.writeString(file.toPath(), contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    }

    private fun resolveFile(path: String): Pair<File, File> {
        val bundleName = path.substringBefore("/")
        val remainingPath = path.substringAfter("/", "")
        require(bundleName.isNotEmpty() && remainingPath.isNotEmpty()) { "Invalid resource path: $path" }
        val baseDir = bundleDatabase.getBundle(bundleName)?.dir?.canonicalFile
            ?: throw IllegalArgumentException("Failed to find bundle: $bundleName")
        val file = baseDir.resolve(remainingPath).canonicalFile
        if (!file.toPath().startsWith(baseDir.toPath())) {
            throw IllegalArgumentException("Invalid file path: $path")
        }
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
