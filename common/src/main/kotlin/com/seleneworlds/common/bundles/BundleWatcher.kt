package com.seleneworlds.common.bundles

import org.slf4j.Logger
import com.seleneworlds.common.data.BundleDrivenRegistry
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.util.Disposable
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class WatchContext(
    val bundle: Bundle,
    val basePath: Path
)

private data class FileState(
    val size: Long,
    val lastModifiedTime: FileTime
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
    private val fileStates = ConcurrentHashMap<String, FileState>()

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
            logger.error("Bundle directory does not exist: ${bundleDir.absolutePath}")
            return
        }

        try {
            val rootPath = bundleDir.toPath()
            registerDirectoryWatch(rootPath, bundle)
            syncedBundleRoots.forEach { rootName ->
                val watchedRootPath = rootPath.resolve(rootName)
                if (Files.isDirectory(watchedRootPath)) {
                    registerDirectoryTree(watchedRootPath, bundle)
                }
            }

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
                processPendingBundleUpdates(bundleId, updatedFiles, deletedFiles)
                logger.info(
                    "Processed content update for bundle {}: +{}, -{}",
                    bundleId, updatedFiles.size, deletedFiles.size
                )
            }
        }
    }

    protected open fun processPendingBundleUpdates(bundleId: String, updatedFiles: Set<String>, deletedFiles: Set<String>) {
        val bundle = bundleDatabase.getBundle(bundleId)
        if (bundle != null) {
            for (filePath in updatedFiles) {
                val registry = findRegistryForFile(filePath) ?: continue
                try {
                    registry.bundleFileUpdated(bundleDatabase, bundle, filePath)
                } catch (e: Exception) {
                    logger.error(
                        "Failed to process updated registry file {} for bundle {}",
                        filePath,
                        bundleId,
                        e
                    )
                }
            }

            for (filePath in deletedFiles) {
                val registry = findRegistryForFile(filePath) ?: continue
                try {
                    registry.bundleFileRemoved(bundleDatabase, bundle, filePath)
                } catch (e: Exception) {
                    logger.error(
                        "Failed to process removed registry file {} for bundle {}",
                        filePath,
                        bundleId,
                        e
                    )
                }
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

        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(filePath)) {
            handleDirectoryCreated(filePath, context.bundle)
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
        val fileKey = getFileStateKey(bundle, relativePath)
        val currentState = readFileState(filePath) ?: return
        val previousState = fileStates[fileKey]

        if (previousState == null) {
            fileStates[fileKey] = currentState
            return
        }

        if (currentState != previousState) {
            fileStates[fileKey] = currentState
            pendingUpdates.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
            pendingDeletes[bundle.manifest.name]?.remove(relativePath)
        }
    }

    private fun handleBundleContentFileCreated(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath).toString()
        val fileKey = getFileStateKey(bundle, relativePath)
        val currentState = readFileState(filePath) ?: return
        fileStates[fileKey] = currentState

        pendingUpdates.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
        pendingDeletes[bundle.manifest.name]?.remove(relativePath)
        logger.debug("New file created: $relativePath")
    }

    private fun handleBundleContentFileDeleted(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath).toString()
        val fileKey = getFileStateKey(bundle, relativePath)

        fileStates.remove(fileKey)

        pendingUpdates[bundle.manifest.name]?.remove(relativePath)
        pendingDeletes.computeIfAbsent(bundle.manifest.name) { ConcurrentHashMap.newKeySet() }.add(relativePath)
        logger.debug("File deleted: $relativePath")
    }

    private fun isSyncedBundleContentFile(bundle: Bundle, filePath: Path): Boolean {
        val relativePath = bundle.dir.toPath().relativize(filePath)
        return syncedBundleContentFilePattern.containsMatchIn(relativePath.toString().replace('\\', '/'))
    }

    private fun getFileStateKey(bundle: Bundle, relativePath: String): String {
        return "${bundle.manifest.name}:$relativePath"
    }

    private fun handleDirectoryCreated(dirPath: Path, bundle: Bundle) {
        if (!shouldWatchDirectory(bundle, dirPath)) {
            logger.debug("Ignoring new directory outside watched bundle roots: {}", dirPath)
            return
        }

        logger.info("New directory created: {} - registering subtree for watch", dirPath)
        registerDirectoryTree(dirPath, bundle)
    }

    abstract fun getRegistry(name: String): Registry<*>?

    private fun readFileState(filePath: Path): FileState? {
        return try {
            val fileAttributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            FileState(
                size = fileAttributes.size(),
                lastModifiedTime = fileAttributes.lastModifiedTime()
            )
        } catch (e: Exception) {
            logger.warn("Failed to read file state for: $filePath", e)
            null
        }
    }

    private fun registerDirectoryTree(rootPath: Path, bundle: Bundle) {
        var directoryCount = 0

        Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!shouldWatchDirectory(bundle, dir)) {
                    return FileVisitResult.SKIP_SUBTREE
                }

                registerDirectoryWatch(dir, bundle)
                directoryCount++
                return FileVisitResult.CONTINUE
            }
        })

        logger.debug("Registered {} directories in watched subtree {}", directoryCount, rootPath)
    }

    private fun shouldWatchDirectory(bundle: Bundle, dirPath: Path): Boolean {
        val normalizedRelativePath = bundle.dir.toPath().relativize(dirPath).toString().replace('\\', '/')
        if (normalizedRelativePath.isEmpty()) {
            return true
        }

        val segments = normalizedRelativePath.split('/')
        if (segments.any { it.startsWith(".") }) {
            return false
        }

        return segments.first() in syncedBundleRoots
    }

    override fun dispose() {
        stopWatching()
    }

    companion object {
        private val syncedBundleRoots = setOf("common", "client")
        private val syncedBundleContentFilePattern = "^(?!.*/\\.|.*~$)(common|client)/.+".toRegex()
        private val registryFilePattern = "^(common|client)/data/[\\w-]+/([\\w-]+)(?:/.*|\\.json)$".toRegex()
    }
}
