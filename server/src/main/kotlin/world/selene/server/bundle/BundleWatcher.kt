package world.selene.server.bundle

import org.slf4j.Logger
import world.selene.common.bundles.Bundle
import world.selene.common.bundles.BundleDatabase
import world.selene.common.network.packet.NotifyAssetUpdatePacket
import world.selene.common.util.Disposable
import world.selene.server.network.NetworkServer
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class WatchContext(
    val bundle: Bundle,
    val basePath: Path
)

class BundleWatcher(
    private val logger: Logger,
    private val bundleDatabase: BundleDatabase,
    private val networkServer: NetworkServer
) : Disposable {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = ConcurrentHashMap<WatchKey, WatchContext>()
    private var isRunning = false
    private var watchThread: Thread? = null
    
    private val pendingChanges = ConcurrentHashMap<String, BundleChanges>()
    
    data class BundleChanges(
        val updated: MutableSet<String> = mutableSetOf(),
        val deleted: MutableSet<String> = mutableSetOf()
    )

    private val syncedBundleContentFilePattern = "^(?!.*/\\.|.*~$)(common|client)/.+".toRegex()

    fun startWatching() {
        if (isRunning) {
            throw IllegalStateException("Bundle watcher is already running.")
        }

        for (bundle in bundleDatabase.loadedBundles) {
            registerBundleWatch(bundle)
        }

        watchThread = thread(name = "BundleWatcher", isDaemon = true) {
            watchLoop()
        }
    }

    fun stopWatching() {
        logger.info("Stopping bundle watcher")
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
            Files.walk(rootPath)
                .filter { Files.isDirectory(it) }
                .forEach { dirPath ->
                    registerDirectoryWatch(dirPath, bundle)
                }
            
            logger.debug("Registered watch for bundle {} at {}", bundle.manifest.name, bundleDir)
        } catch (e: Exception) {
            logger.error("Failed to register watch for bundle ${bundle.manifest.name}", e)
        }
    }

    fun sendPendingUpdates() {
        if (pendingChanges.isEmpty()) {
            return
        }
        
        val updates = pendingChanges.toList()
        pendingChanges.clear()
        
        for ((bundleId, changes) in updates) {
            if (changes.updated.isNotEmpty() || changes.deleted.isNotEmpty()) {
                val packet = NotifyAssetUpdatePacket(
                    bundleId = bundleId,
                    updated = changes.updated.toList(),
                    deleted = changes.deleted.toList()
                )
                networkServer.clients.forEach { it.send(packet) }
                logger.info("Sent content update notification for bundle {}: +{}, -{}",
                    bundleId, changes.updated.size, changes.deleted.size)
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
        val relativePath = bundle.dir.toPath().relativize(filePath)
        val changes = pendingChanges.computeIfAbsent(bundle.dir.name) { BundleChanges() }
        changes.updated.add(relativePath.toString())
        changes.deleted.remove(relativePath.toString())
    }

    private fun handleBundleContentFileCreated(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath)
        val changes = pendingChanges.computeIfAbsent(bundle.dir.name) { BundleChanges() }
        changes.updated.add(relativePath.toString())
        changes.deleted.remove(relativePath.toString())
    }

    private fun handleBundleContentFileDeleted(filePath: Path, bundle: Bundle) {
        val relativePath = bundle.dir.toPath().relativize(filePath)
        val changes = pendingChanges.computeIfAbsent(bundle.dir.name) { BundleChanges() }
        changes.updated.remove(relativePath.toString())
        changes.deleted.add(relativePath.toString())
    }

    private fun isSyncedBundleContentFile(bundle: Bundle, filePath: Path): Boolean {
        val relativePath = bundle.dir.toPath().relativize(filePath)
        return syncedBundleContentFilePattern.containsMatchIn(relativePath.toString())
    }

    override fun dispose() {
        stopWatching()
    }
}
