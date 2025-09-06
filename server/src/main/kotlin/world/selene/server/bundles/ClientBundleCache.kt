package world.selene.server.bundles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import world.selene.server.config.ServerConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ClientBundleCache(config: ServerConfig) {
    private val bundlesDir = File(config.bundlesPath)
    private val cacheDir = File("cache", "client_bundles")
    private val hashByBundleName = ConcurrentHashMap<String, String>()

    fun createClientZip(bundleDir: File, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val bundlePath = bundleDir.toPath()
        ZipOutputStream(outputFile.outputStream()).use { zipOut ->
            Files.walkFileTree(bundlePath, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relPath = bundlePath.relativize(file).toString()
                    if (relPath.startsWith("server") || relPath.contains("${File.separator}server")) {
                        return FileVisitResult.CONTINUE
                    }
                    if (relPath.startsWith(".")) {
                        return FileVisitResult.CONTINUE
                    }
                    val entry = ZipEntry(relPath)
                    zipOut.putNextEntry(entry)
                    Files.newInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relPath = bundlePath.relativize(dir).toString()
                    if (relPath == ".") return FileVisitResult.CONTINUE
                    if (relPath == "server" || relPath.contains("${File.separator}server")) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    private fun hashBundleDir(bundleDir: File): String {
        val hasher = MessageDigest.getInstance("SHA-256")
        val files = mutableListOf<Path>()
        Files.walkFileTree(bundleDir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                files.add(file)
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(
                file: Path,
                exc: IOException
            ): FileVisitResult {
                if (exc is NoSuchFileException) {
                    return FileVisitResult.CONTINUE
                } else if (exc is AccessDeniedException) {
                    return FileVisitResult.CONTINUE
                }
                throw exc
            }
        })
        files.sortBy { it.toString() }
        for (file in files) {
            try {
                FileInputStream(file.toFile()).use { fis ->
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

    fun watchBundles(bundles: List<String>) {
        val watcher = FileSystems.getDefault().newWatchService()
        val bundleDirs = bundles.map { File(bundlesDir, it) }
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
                println("Failed to watch $dir: ${e.message}")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val key = watcher.take() ?: break
                if (key.pollEvents().isNotEmpty()) {
                    bundleByWatchKey[key]?.let { bundleDir ->
                        val hash = hashBundleDir(bundleDir)
                        hashByBundleName[bundleDir.name] = hash
                        println("Recomputed hash for ${bundleDir.name}: $hash")
                    }
                }
                key.reset()
            }
        }
    }

    fun getHash(bundleDir: File): String? {
        return hashByBundleName.getOrPut(bundleDir.name) {
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

        return zipFile
    }

    fun hasClientSide(bundleDir: File): Boolean {
        return bundleDir.resolve("common").exists() || bundleDir.resolve("client").exists()
    }
}