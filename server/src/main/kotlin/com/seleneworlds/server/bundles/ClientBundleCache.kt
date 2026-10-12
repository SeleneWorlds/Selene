package com.seleneworlds.server.bundles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.Logger
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.bundleContentFiles
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ClientBundleCache(config: ServerConfig, private val logger: Logger) {
    private val directoriesByBundleName = ConcurrentHashMap<String, File>()
    private val cacheDir = File("cache", "client_bundles")
    private val hashByBundleName = ConcurrentHashMap<String, String>()
    private val cachedZipByBundleName = ConcurrentHashMap<String, File>()

    fun createClientZip(bundleDir: File, outputFile: File) {
        outputFile.parentFile.mkdirs()
        ZipOutputStream(outputFile.outputStream()).use { zipOut ->
            for (file in bundleContentFiles(bundleDir, listOf("common", "client"))) {
                val relPath = file.relativeTo(bundleDir).invariantSeparatorsPath
                if (relPath.split('/').any { it.startsWith(".") }) continue
                zipOut.putNextEntry(ZipEntry(relPath))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }

    private fun hashBundleDir(bundleDir: File): String {
        val hasher = MessageDigest.getInstance("SHA-256")
        val files = bundleContentFiles(bundleDir).sortedBy { it.invariantSeparatorsPath }.toList()
        for (file in files) {
            try {
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        hasher.update(buffer, 0, read)
                    }
                }
            } catch (e: Exception) {
                if (e is FileNotFoundException || e is NoSuchFileException || e is AccessDeniedException) {
                    continue
                }
                throw e
            }
        }
        return hasher.digest().joinToString("") { "%02x".format(it) }
    }

    fun clearCacheForBundle(bundleName: String) {
        val cacheKey = directoriesByBundleName[bundleName]?.canonicalPath ?: bundleName
        // Remove old zip file if it exists
        cachedZipByBundleName.remove(cacheKey)?.let { oldZipFile ->
            try {
                if (oldZipFile.exists()) {
                    oldZipFile.delete()
                }
            } catch (e: Exception) {
                logger.error("Failed to clear cached zip file for bundle $bundleName", e)
            }
        }
        
        // Remove hash entry to force recalculation
        hashByBundleName.remove(cacheKey)
    }

    fun cleanupStaleZips() {
        val currentHashes = hashByBundleName.values.toSet()
        for (file in cacheDir.listFiles() ?: emptyArray()) {
            if (file.isDirectory || file.extension != "zip") {
                continue
            }
            // Check if this zip file corresponds to a current hash
            if (file.nameWithoutExtension !in currentHashes) {
                try {
                    file.delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun watchBundles(bundles: List<Bundle>) {
        val watcher = FileSystems.getDefault().newWatchService()
        bundles.forEach { directoriesByBundleName[it.manifest.name] = it.dir }
        val bundleDirs = bundles.map { it.dir }
        val bundleByWatchKey = mutableMapOf<WatchKey, File>()
        for (dir in bundleDirs) {
            try {
                bundleByWatchKey[dir.toPath().register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                )] = dir
            } catch (e: Exception) {
                logger.warn("Failed to watch $dir: ${e.message}")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val key = watcher.take() ?: break
                if (key.pollEvents().isNotEmpty()) {
                    bundleByWatchKey[key]?.let { bundleDir ->
                        val oldHash = hashByBundleName[bundleDir.canonicalPath]
                        val newHash = hashBundleDir(bundleDir)
                        
                        // Clear cache if bundle content changed
                        if (oldHash != null && oldHash != newHash) {
                            directoriesByBundleName.entries.firstOrNull { it.value == bundleDir }?.key?.let(::clearCacheForBundle)
                        }
                        
                        hashByBundleName[bundleDir.canonicalPath] = newHash
                        logger.debug("Recomputed hash for ${bundleDir.name}: $newHash")
                    }
                }
                key.reset()
            }
        }
    }

    fun getHash(bundleDir: File): String? {
        return hashByBundleName.getOrPut(bundleDir.canonicalPath) {
            hashBundleDir(bundleDir)
        }
    }

    fun getZipFile(bundleDir: File): File {
        val currentHash =
            getHash(bundleDir) ?: throw IllegalStateException("Could not compute hash for ${bundleDir.name}")
        val zipFile = File(cacheDir, "$currentHash.zip")

        if (!zipFile.exists()) {
            cacheDir.mkdirs()
            createClientZip(bundleDir, zipFile)
        }

        // Track the zip file for this bundle
        cachedZipByBundleName[bundleDir.canonicalPath] = zipFile

        return zipFile
    }

    fun hasClientSide(bundleDir: File): Boolean {
        return bundleDir.resolve("common").exists() || bundleDir.resolve("client").exists()
    }
}