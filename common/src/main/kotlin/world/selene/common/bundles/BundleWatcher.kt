package world.selene.common.bundles

import org.slf4j.Logger
import world.selene.common.data.BundleDrivenRegistry
import world.selene.common.data.Registry
import world.selene.common.util.Disposable
import java.nio.file.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class WatchContext(
    val bundle: Bundle,
    val basePath: Path
)

abstract class BundleWatcher(
    protected val logger: Logger,
    protected val bundleDatabase: BundleDatabase
) : Disposable {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = ConcurrentHashMap<WatchKey, WatchContext>()
    protected var isRunning = false
    private var watchThread: Thread? = null

    private val pendingUpdates = ConcurrentHashMap<String, MutableSet<String>>()
    private val pendingDeletes = ConcurrentHashMap<String, MutableSet<String>>()
    private val fileHashes = ConcurrentHashMap<String, String>()

    protected abstract fun onChangeDetected(bundleId: String, updated: Set<String>, deleted: Set<String>)

    fun findRegistryForFile(filePath: String): BundleDrivenRegistry? {
        val normalizedFilePath = filePath.replace('\\', '/')
        return (registryFilePattern.find(normalizedFilePath)?.groups[2]?.value?.let(::getRegistry) as? BundleDrivenRegistry)
    }

    fun startWatching() {
        if (isRunning) {
            throw IllegalStateException("Bundle watcher is already running.")
        }

        for (bundle in bundleDatabase.loadedBundles) {
            registerBundleWatch(bundle)
        }

        isRunning = true
        watchThread = thread(name = "BundleWatcher", isDaemon = true) {
            watchLoop()
        }
    }

    fun stopWatching() {
        logger.info("Stopping bundle watcher")
        isRunning = false
        watchThread?.interrupt()
        watchService.close()
        watchKeys.clear()
    }

    private fun registerDirectoryWatch(dirPath: Path, bundle: Bundle) {
        try {
            val watchKey = dirPath.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            watchKeys[watchKey] = WatchContext(bundle, dirPath)
            logger.debug("Registered watch for directory: {}", dirPath)
        } catch (e: Exception) {
            logger.error("Failed to register watch for directory: {}", dirPath, e)
        }
    }

    private fun registerBundleWatch(bundle: Bundle) {
        val bundleDir = bundle.dir
        if (!bundleDir.exists()) {
            logger.debug("Bundle directory does not exist: ${bundleDir.absolutePath}")
            return
        }

        try {
            val rootPath = bundleDir.toPath()
            Files.walk(rootPath).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .forEach { dirPath ->
                        registerDirectoryWatch(dirPath, bundle)
                    }
            }

            // Initialize file hashes for existing files
            initializeFileHashes(bundle)

            logger.debug("Registered watch for bundle {} at {}", bundle.manifest.name, bundleDir)
        } catch (e: Exception) {
            logger.error("Failed to register watch for bundle ${bundle.manifest.name}", e)
        }
    }

    fun processPendingUpdates() {
        if (pendingUpdates.isEmpty() && pendingDeletes.isEmpty()) {
            return
        }

        val dirtyBundleIds = mutableSetOf<String>()
        dirtyBundleIds.addAll(pendingUpdates.keys)
        dirtyBundleIds.addAll(pendingDeletes.keys)

        for (bundleId in dirtyBundleIds) {
            val updatedFiles = pendingUpdates.remove(bundleId)?.toSet() ?: emptySet()
            val deletedFiles = pendingDeletes.remove(bundleId)?.toSet() ?: emptySet()

            if (updatedFiles.isNotEmpty() || deletedFiles.isNotEmpty()) {
                // Notify registries of changes first
                val bundle = bundleDatabase.getBundle(bundleId)
                if (bundle != null) {
                    // Handle updated files
                    for (filePath in updatedFiles) {
                        findRegistryForFile(filePath)?.bundleFileUpdated(bundleDatabase, bundle, filePath)
                    }

                    // Handle deleted files
                    for (filePath in deletedFiles) {
                        findRegistryForFile(filePath)?.bundleFileRemoved(bundleDatabase, bundle, filePath)
                    }
                }

                // Let subclass handle the change notification
                onChangeDetected(bundleId, updatedFiles, deletedFiles)

                logger.info(
                    "Processed content update for bundle {}: +{}, -{}",
                    bundleId, updatedFiles.size, deletedFiles.size
                )
            }
        }
    }

    private fun watchLoop() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                val key = watchService.take()
                val context = watchKeys[key]
                if (context == null) {
                    logger.warn("Received watch event for unknown key")
                    key.reset()
                    continue
                }

                val events = key.pollEvents()
                for (event in events) {
                    handleWatchEvent(event, context)
                }

                val valid = key.reset()
                if (!valid) {
                    logger.warn("Watch key became invalid for directory {}", context.basePath)
                    watchKeys.remove(key)
                }
            }
        } catch (_: InterruptedException) {
        } catch (_: ClosedWatchServiceException) {
        } catch (e: Exception) {
            logger.error("Error in bundle watcher", e)
        }
    }

    private fun handleWatchEvent(event: WatchEvent<*>, context: WatchContext) {
        val kind = event.kind()
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            logger.warn("Watch event overflow for bundle ${context.bundle.manifest.name}")
            return
        }

        val fileName = event.context() as Path
        val filePath = context.basePath.resolve(fileName)

        // Handle new directory creation - register it for watching
        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(filePath)) {
            logger.info("New directory created: {} - registering for watch", filePath)
            registerDirectoryWatch(filePath, context.bundle)
            return
        }

        if (isSyncedBundleContentFile(context.bundle, filePath)) {
            logger.info("Detected changes in $filePath of bundle ${context.bundle.manifest.name}")

            when (kind) {
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    handleBundleContentFileModified(filePath, context.bundle)
                }

                StandardWatchEventKinds.ENTRY_CREATE -> {
                    handleBundleContentFileCreated(filePath, context.bundle)
                }

                StandardWatchEventKinds.ENTRY_DELETE -> {
                    handleBundleContentFileDeleted(filePath, context.bundle)
                }

                else -> {
                    logger.debug("Ignoring event {} for {}", kind.name(), filePath)
                }
            }
        }
    }

    private fun handleBundleContentFileModified(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath).toString()
        val fileKey = getFileHashKey(bundle, relativePath)

        // Calculate current hash and compare with stored hash
        val currentHash = calculateFileHash(filePath)
        val previousHash = fileHashes[fileKey]

        // Only send update if hash actually changed
        if (previousHash == null || currentHash != previousHash) {
            fileHashes[fileKey] = currentHash
            pendingUpdates.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
            pendingDeletes[bundle.manifest.name]?.remove(relativePath)
            logger.debug("File content actually changed: $relativePath (hash: ${currentHash.take(8)}...)")
        } else {
            logger.debug("File content unchanged, ignoring update: $relativePath")
        }
    }

    private fun handleBundleContentFileCreated(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath).toString()
        val fileKey = getFileHashKey(bundle, relativePath)

        // Calculate hash for new file
        val currentHash = calculateFileHash(filePath)
        fileHashes[fileKey] = currentHash

        pendingUpdates.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
        pendingDeletes[bundle.manifest.name]?.remove(relativePath)
        logger.debug("New file created: $relativePath (hash: ${currentHash.take(8)}...)")
    }

    private fun handleBundleContentFileDeleted(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath).toString()
        val fileKey = getFileHashKey(bundle, relativePath)

        // Remove hash for deleted file
        fileHashes.remove(fileKey)

        pendingUpdates[bundle.manifest.name]?.remove(relativePath)
        pendingDeletes.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
        logger.debug("File deleted: $relativePath")
    }

    private fun isSyncedBundleContentFile(bundle: Bundle, filePath: Path): Boolean {
        val relativePath = bundle.dir.toPath().relativize(filePath)
        return syncedBundleContentFilePattern.containsMatchIn(relativePath.toString().replace('\\', '/'))
    }

    private fun getFileHashKey(bundle: Bundle, relativePath: String): String {
        return "${bundle.manifest.name}:$relativePath"
    }

    private fun initializeFileHashes(bundle: Bundle) {
        try {
            val bundleDir = bundle.dir.toPath()
            var count = 0
            Files.walk(bundleDir).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { isSyncedBundleContentFile(bundle, it) }
                    .forEach { filePath ->
                        val relativePath = bundleDir.relativize(filePath).toString()
                        val fileKey = getFileHashKey(bundle, relativePath)
                        val hash = calculateFileHash(filePath)
                        fileHashes[fileKey] = hash
                        count++
                    }
            }
            logger.debug("Initialized hashes for ${count} files in bundle ${bundle.manifest.name}")
        } catch (e: Exception) {
            logger.error("Failed to initialize file hashes for bundle ${bundle.manifest.name}", e)
        }
    }

    abstract fun getRegistry(name: String): Registry<*>?

    private fun calculateFileHash(filePath: Path): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = Files.readAllBytes(filePath)
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.warn("Failed to calculate hash for file: $filePath", e)
            ""
        }
    }

    override fun dispose() {
        stopWatching()
    }

    companion object {
        private val syncedBundleContentFilePattern = "^(?!.*/\\.|.*~$)(common|client)/.+".toRegex()
        private val registryFilePattern = "^(common|client)/data/[\\w-]+/([\\w-]+)/.*".toRegex()
    }
}
