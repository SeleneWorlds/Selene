package com.seleneworlds.common.bundles

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BundleLifecycleManagerTest {

    @Test
    fun `reloadActiveBundles clears enabled bundles in reverse order and rebuilds runtime`() {
        withLifecycleFixture { fixture ->
            val base = fixture.addBundle("base")
            val addon = fixture.addBundle("addon", dependencies = listOf("base"))
            fixture.addBundle("disabled", enabled = false)

            fixture.lifecycleManager.reloadActiveBundles()

            assertEquals(listOf("addon", "base"), fixture.cleanedBundles)
            assertEquals(listOf("base", "addon"), fixture.database.enabledBundles.map { it.manifest.name })
            assertEquals(listOf("rebuild:base,addon", "rebuilt"), fixture.runtimeEvents)

            assertEquals(base, fixture.database.getEnabledBundle("base"))
            assertEquals(addon, fixture.database.getEnabledBundle("addon"))
        }
    }

    @Test
    fun `reloadBundleClosure uses the same clear and rebuild lifecycle`() {
        withLifecycleFixture { fixture ->
            fixture.addBundle("base")
            fixture.addBundle("direct", dependencies = listOf("base"))
            fixture.addBundle("transitive", dependencies = listOf("direct"))
            fixture.addBundle("other")

            fixture.lifecycleManager.reloadBundleClosure("base", emptySet())

            assertEquals(listOf("transitive", "direct", "base"), fixture.cleanedBundles)
            assertEquals(
                listOf("base", "direct", "transitive", "other"),
                fixture.database.enabledBundles.map { it.manifest.name }
            )
            assertEquals(listOf("rebuild:base,direct,transitive,other", "rebuilt"), fixture.runtimeEvents)
        }
    }

    private fun withLifecycleFixture(block: (LifecycleFixture) -> Unit) {
        val tempDir = Files.createTempDirectory("bundle-lifecycle-manager-test").toFile()
        try {
            val database = BundleDatabase()
            val cleanedBundles = mutableListOf<String>()
            val runtimeEvents = mutableListOf<String>()
            val loader = object : BundleLifecycleOperations {
                override fun preloadBundleModules(bundle: Bundle) = Unit

                override fun clearBundleState(bundle: Bundle, deletedFiles: Set<String>) {
                    cleanedBundles.add(bundle.manifest.name)
                }

                override fun runBundleEntrypoints(bundles: List<Bundle>, entrypointFilters: List<String>) = Unit
            }
            val runtimeRebuilder = object : BundleRuntimeRebuilder {
                override val entrypointFilters: List<String> = emptyList()

                override fun rebuildActiveBundles(bundleDatabase: BundleDatabase) {
                    runtimeEvents.add(
                        "rebuild:${bundleDatabase.enabledBundles.joinToString(",") { it.manifest.name }}"
                    )
                }

                override fun onRuntimeRebuilt() {
                    runtimeEvents.add("rebuilt")
                }
            }
            val lifecycleManager = BundleLifecycleManager(
                logger = LoggerFactory.getLogger(BundleLifecycleManagerTest::class.java),
                bundleLoader = loader,
                bundleDatabase = database,
                runtimeRebuilder = runtimeRebuilder
            )

            block(LifecycleFixture(tempDir, database, lifecycleManager, cleanedBundles, runtimeEvents))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private data class LifecycleFixture(
        val tempDir: File,
        val database: BundleDatabase,
        val lifecycleManager: BundleLifecycleManager,
        val cleanedBundles: List<String>,
        val runtimeEvents: List<String>
    ) {
        fun addBundle(
            name: String,
            dependencies: List<String> = emptyList(),
            enabled: Boolean = true
        ): Bundle {
            val bundleDir = File(tempDir, name).apply { mkdirs() }
            val bundle = Bundle(BundleManifest(name = name, dependencies = dependencies), bundleDir)
            database.addBundle(bundle, enabled = enabled)
            return bundle
        }
    }
}
