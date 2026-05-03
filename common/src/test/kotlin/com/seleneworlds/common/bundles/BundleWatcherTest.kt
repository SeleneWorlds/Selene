package com.seleneworlds.common.bundles

import org.slf4j.helpers.NOPLogger
import com.seleneworlds.common.data.BundleDrivenRegistry
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.data.mappings.NameIdRegistry
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class BundleWatcherTest {

    @Test
    fun ignoresChangesOutsideCommonAndClientRoots() {
        withWatcherBundle { bundle, watcher ->
            val commonFile = writeFile(bundle.dir.toPath().resolve("common/assets/test/atlas.txt"), "common-1")
            val serverFile = writeFile(bundle.dir.toPath().resolve("server/assets/test/server.txt"), "server-1")

            watcher.startWatching()
            watcher.resetProcessedUpdates()

            Files.writeString(serverFile, "server-2")
            assertEquals(watcher.awaitProcessedUpdate(), null, "server changes should not trigger bundle updates")

            Files.writeString(commonFile, "common-2")
            assertEquals(watcher.awaitProcessedUpdate(), null, "first modify should establish baseline for existing files")

            watcher.resetProcessedUpdates()
            Files.writeString(commonFile, "common-3")
            val update = assertNotNull(watcher.awaitProcessedUpdate())
            assertEquals(setOf("common/assets/test/atlas.txt"), update.updatedFiles)
            assertTrue(update.deletedFiles.isEmpty())
        }
    }

    @Test
    fun usesFirstModifyToEstablishBaselineForExistingFiles() {
        withWatcherBundle { bundle, watcher ->
            val filePath = writeFile(bundle.dir.toPath().resolve("common/assets/test/atlas.txt"), "content")

            watcher.startWatching()
            watcher.resetProcessedUpdates()

            invokeModifiedHandler(watcher, filePath, bundle)
            watcher.processPendingUpdates()

            assertTrue(watcher.processedUpdates.isEmpty(), "unchanged file metadata should not queue updates")
        }
    }

    @Test
    fun tracksFileCreateAndDeleteEvents() {
        withWatcherBundle { bundle, watcher ->
            val filePath = bundle.dir.toPath().resolve("client/assets/test/new.txt")
            filePath.parent.createDirectories()

            watcher.startWatching()
            watcher.resetProcessedUpdates()

            Files.writeString(filePath, "created")
            val createdUpdate = assertNotNull(watcher.awaitProcessedUpdate())
            assertEquals(setOf("client/assets/test/new.txt"), createdUpdate.updatedFiles)
            assertTrue(createdUpdate.deletedFiles.isEmpty())

            watcher.resetProcessedUpdates()
            Files.delete(filePath)
            val deletedUpdate = assertNotNull(watcher.awaitProcessedUpdate())
            assertTrue(deletedUpdate.updatedFiles.isEmpty())
            assertEquals(setOf("client/assets/test/new.txt"), deletedUpdate.deletedFiles)
        }
    }

    @Test
    fun recursivelyRegistersNewDirectoriesAndEstablishesBaselineLazily() {
        withWatcherBundle { bundle, watcher ->
            bundle.dir.toPath().resolve("common").createDirectories()

            watcher.startWatching()
            watcher.resetProcessedUpdates()

            val importedTree = createTempDirectory("imported-tree")
            try {
                writeFile(importedTree.resolve("nested/existing.txt"), "seeded")
                val targetDir = bundle.dir.toPath().resolve("common/imported")
                importedTree.moveTo(targetDir)

                assertTrue(
                    watcher.awaitCondition {
                        watcher.isWatchingDirectory(targetDir) && watcher.isWatchingDirectory(targetDir.resolve("nested"))
                    },
                    "new directory subtree should be registered recursively"
                )

                watcher.resetProcessedUpdates()
                invokeModifiedHandler(watcher, targetDir.resolve("nested/existing.txt"), bundle)
                watcher.processPendingUpdates()
                assertTrue(
                    watcher.processedUpdates.isEmpty(),
                    "existing files inside newly created directories should establish baseline on first modify"
                )

                Files.writeString(targetDir.resolve("nested/existing.txt"), "updated")
                val update = assertNotNull(watcher.awaitProcessedUpdate())
                assertEquals(setOf("common/imported/nested/existing.txt"), update.updatedFiles)
                assertTrue(update.deletedFiles.isEmpty())
            } finally {
                if (Files.exists(importedTree)) {
                    importedTree.deleteRecursively()
                }
            }
        }
    }

    @Test
    fun resolvesMergedRegistryFilesToTheirRegistry() {
        val registry = TestRegistry()
        val watcher = object : RecordingBundleWatcher(BundleDatabase()) {
            override fun getRegistry(name: String): Registry<*>? = if (name == "tiles") registry else null
        }

        val directoryRegistry: BundleDrivenRegistry? = watcher.findRegistryForFile("common/data/example/tiles/path/to/entry.json")
        val mergedRegistry: BundleDrivenRegistry? = watcher.findRegistryForFile("common/data/example/tiles.json")

        assertSame(registry, directoryRegistry)
        assertSame(registry, mergedRegistry)
    }

    private fun withWatcherBundle(block: (Bundle, RecordingBundleWatcher) -> Unit) {
        val rootDir = createTempDirectory("bundle-watcher-test")
        val bundle = Bundle(BundleManifest(name = rootDir.name), rootDir.toFile())
        val bundleDatabase = BundleDatabase().apply { addBundle(bundle) }
        val watcher = RecordingBundleWatcher(bundleDatabase)

        try {
            block(bundle, watcher)
        } finally {
            watcher.stopWatching()
            rootDir.deleteRecursively()
        }
    }

    private fun writeFile(path: Path, content: String): Path {
        path.parent?.createDirectories()
        path.writeText(content)
        return path
    }

    private fun invokeModifiedHandler(watcher: RecordingBundleWatcher, filePath: Path, bundle: Bundle) {
        val method = BundleWatcher::class.java.getDeclaredMethod(
            "handleBundleContentFileModified",
            Path::class.java,
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(watcher, filePath, bundle)
    }

    private open class RecordingBundleWatcher(bundleDatabase: BundleDatabase) :
        BundleWatcher(NOPLogger.NOP_LOGGER, bundleDatabase) {

        val processedUpdates = mutableListOf<ProcessedUpdate>()

        override fun getRegistry(name: String): Registry<*>? = null

        override fun processPendingBundleUpdates(
            bundleId: String,
            updatedFiles: Set<String>,
            deletedFiles: Set<String>
        ) {
            processedUpdates += ProcessedUpdate(bundleId, updatedFiles, deletedFiles)
        }

        fun resetProcessedUpdates() {
            processedUpdates.clear()
        }

        fun awaitProcessedUpdate(timeoutMs: Long = 1500L): ProcessedUpdate? {
            return if (awaitCondition(timeoutMs) {
                    processPendingUpdates()
                    processedUpdates.isNotEmpty()
                }
            ) {
                processedUpdates.first()
            } else {
                null
            }
        }

        fun awaitCondition(timeoutMs: Long = 1500L, condition: () -> Boolean): Boolean {
            val deadline = System.nanoTime() + timeoutMs * 1_000_000
            while (System.nanoTime() < deadline) {
                if (condition()) {
                    return true
                }
                Thread.sleep(25)
            }
            return condition()
        }

        fun isWatchingDirectory(path: Path): Boolean {
            val field = BundleWatcher::class.java.getDeclaredField("watchKeys")
            field.isAccessible = true
            val watchKeys = field.get(this) as Map<*, *>
            return watchKeys.values.any { context ->
                val basePathField = context!!::class.java.getDeclaredField("basePath")
                basePathField.isAccessible = true
                basePathField.get(context) == path
            }
        }
    }

    private data class ProcessedUpdate(
        val bundleId: String,
        val updatedFiles: Set<String>,
        val deletedFiles: Set<String>
    )

    private class TestRegistry : Registry<Any>, BundleDrivenRegistry {
        override val dataType = Any::class
        override val name = "tiles"
        override fun get(id: Int): Any? = null
        override fun getIdentifier(id: Int): Identifier? = null
        override fun get(identifier: Identifier): Any? = null
        override fun getId(identifier: Identifier): Int = -1
        override fun getAll(): Map<Identifier, Any> = emptyMap()
        override fun findByMetadata(key: String, value: Any): Pair<Identifier, Any>? = null
        override fun registryPopulated(mappings: NameIdRegistry, throwOnMissingId: Boolean) = Unit
        override fun subscribe(reference: RegistryReference<Any>, handler: (Any?) -> Unit) = Unit
        override fun unsubscribe(reference: RegistryReference<Any>, handler: (Any?) -> Unit) = Unit
        override fun addReloadListener(listener: RegistryReloadListener<Any>) = Unit
        override fun removeReloadListener(listener: RegistryReloadListener<Any>) = Unit
        override fun load(bundleDatabase: BundleDatabase) = Unit
        override fun bundleFileUpdated(bundleDatabase: BundleDatabase, bundle: Bundle, path: String) = Unit
        override fun bundleFileRemoved(bundleDatabase: BundleDatabase, bundle: Bundle, path: String) = Unit
    }
}
