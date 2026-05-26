package com.seleneworlds.common.bundles

import org.slf4j.LoggerFactory
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

@OptIn(ExperimentalPathApi::class)
class ScriptHotReloadTest {

    @Test
    fun `reloadUpdatedScripts refreshes preloaded modules`() {
        withScriptHotReloadBundle(
            manifest = BundleManifest(
                name = "test-bundle",
                preloads = mapOf("test.client" to "client/test.lua")
            ),
            scriptRoots = setOf("common", "client"),
            entrypointFilters = listOf("common/", "client/", "init.lua")
        ) {
            writeScript(bundle, "client/test.lua", "return 'before'")
            bundleLoader.preloadBundleModules(bundle)

            assertEquals("before", requireString("test.client"))

            writeScript(bundle, "client/test.lua", "return 'after'")
            scriptHotReload.reloadUpdatedScripts(bundle, setOf("client/test.lua"))

            assertEquals("after", requireString("test.client"))
        }
    }

    @Test
    fun `reloadUpdatedScripts invalidates resolver loaded modules`() {
        withScriptHotReloadBundle(
            manifest = BundleManifest(name = "test-bundle"),
            scriptRoots = setOf("common", "client"),
            entrypointFilters = listOf("common/", "client/", "init.lua")
        ) {
            writeScript(bundle, "client/test.lua", "return 'before'")
            bundleLoader.resolveBundles(setOf(bundle.manifest.name))
            bundleDatabase.enableBundle(bundle)

            assertEquals("before", requireString("test-bundle.client.test"))

            writeScript(bundle, "client/test.lua", "return 'after'")
            scriptHotReload.reloadUpdatedScripts(bundle, setOf("client/test.lua"))

            assertEquals("after", requireString("test-bundle.client.test"))
        }
    }

    @Test
    fun `reloadUpdatedScripts reruns matching entrypoints`() {
        withScriptHotReloadBundle(
            manifest = BundleManifest(
                name = "test-bundle",
                entrypoints = listOf("client/entry.lua")
            ),
            scriptRoots = setOf("common", "client"),
            entrypointFilters = listOf("common/", "client/", "init.lua")
        ) {
            writeScript(bundle, "client/entry.lua", "entrypointRuns = (entrypointRuns or 0) + 1")

            scriptHotReload.reloadUpdatedScripts(bundle, setOf("client/entry.lua"))

            assertEquals(1, getGlobalInt("entrypointRuns"))
        }
    }

    @Test
    fun `reloadUpdatedScripts ignores paths outside configured roots`() {
        withScriptHotReloadBundle(
            manifest = BundleManifest(
                name = "test-bundle",
                preloads = mapOf("test.server" to "server/test.lua")
            ),
            scriptRoots = setOf("common", "client"),
            entrypointFilters = listOf("common/", "client/", "init.lua")
        ) {
            writeScript(bundle, "server/test.lua", "return 'before'")
            bundleLoader.preloadBundleModules(bundle)

            assertEquals("before", requireString("test.server"))

            writeScript(bundle, "server/test.lua", "return 'after'")
            scriptHotReload.reloadUpdatedScripts(bundle, setOf("server/test.lua"))

            assertEquals("before", requireString("test.server"))
        }
    }

    @Test
    fun `unloadDeletedScripts removes preloaded modules`() {
        withScriptHotReloadBundle(
            manifest = BundleManifest(
                name = "test-bundle",
                preloads = mapOf("test.client" to "client/test.lua")
            ),
            scriptRoots = setOf("common", "client"),
            entrypointFilters = listOf("common/", "client/", "init.lua")
        ) {
            writeScript(bundle, "client/test.lua", "return 'loaded'")
            bundleLoader.preloadBundleModules(bundle)

            assertEquals("loaded", requireString("test.client"))

            scriptHotReload.unloadDeletedScripts(bundle, setOf("client/test.lua"))

            assertFails {
                requireString("test.client")
            }
        }
    }

    private fun withScriptHotReloadBundle(
        manifest: BundleManifest,
        scriptRoots: Set<String>,
        entrypointFilters: List<String>,
        block: ScriptHotReloadFixture.() -> Unit
    ) {
        val rootDir = Files.createTempDirectory("script-hot-reload-test")
        val bundle = Bundle(manifest, rootDir.toFile())
        val bundleDatabase = BundleDatabase()
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        luaPackage.initialize(luaManager)
        val bundleLoader = BundleLoader(
            logger = LoggerFactory.getLogger(ScriptHotReloadTest::class.java),
            luaManager = luaManager,
            luaPackage = luaPackage,
            bundleDatabase = bundleDatabase,
            bundleLocator = object : BundleLocator {
                override fun locateBundle(name: String): Bundle? = if (name == bundle.manifest.name) bundle else null
            }
        )
        val scriptHotReload = ScriptHotReload(
            bundleLifecycleManager = BundleLifecycleManager(
                logger = LoggerFactory.getLogger(ScriptHotReloadTest::class.java),
                bundleLoader = bundleLoader,
                bundleDatabase = bundleDatabase,
                runtimeRebuilder = object : BundleRuntimeRebuilder {
                    override val entrypointFilters: List<String> = entrypointFilters

                    override fun rebuildActiveBundles(bundleDatabase: BundleDatabase) = Unit
                }
            ),
            bundleLoader = bundleLoader,
            luaManager = luaManager,
            luaPackage = luaPackage,
            logger = LoggerFactory.getLogger(ScriptHotReloadTest::class.java),
            scriptRoots = scriptRoots,
            entrypointFilters = entrypointFilters
        )

        try {
            ScriptHotReloadFixture(bundle, bundleDatabase, bundleLoader, scriptHotReload, luaManager).block()
        } finally {
            luaManager.lua.close()
            rootDir.deleteRecursively()
        }
    }

    private fun writeScript(bundle: Bundle, relativePath: String, script: String) {
        bundle.dir.toPath().resolve(relativePath).apply {
            parent.createDirectories()
            writeText(script)
        }
    }

    private data class ScriptHotReloadFixture(
        val bundle: Bundle,
        val bundleDatabase: BundleDatabase,
        val bundleLoader: BundleLoader,
        val scriptHotReload: ScriptHotReload,
        val luaManager: LuaManager
    ) {
        fun requireString(moduleName: String): String {
            luaManager.lua.getGlobal("require")
            luaManager.lua.push(moduleName)
            luaManager.lua.pCall(1, 1)
            val result = assertNotNull(luaManager.lua.toString(-1))
            luaManager.lua.pop(1)
            return result
        }

        fun getGlobalInt(name: String): Int {
            luaManager.lua.getGlobal(name)
            val value = luaManager.lua.toInteger(-1).toInt()
            luaManager.lua.pop(1)
            return value
        }
    }
}
