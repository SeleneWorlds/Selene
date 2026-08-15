package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.server.config.ServerConfig
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientLuaModulesTest {
    @Test
    fun `index includes client lua modules and excludes server lua modules`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle(
            BundleManifest(
                name = "test-bundle",
                entrypoints = listOf("client/init.lua")
            )
        ) {
            writeText("init.lua", "return 'root'")
            writeText("common/shared.lua", "return 'shared'")
            writeText("client/init.lua", "require('test-bundle.common.shared')")
            writeText("server/secret.lua", "return 'secret'")
        }
        bundleDatabase.addBundle(bundle)

        val luaModules = createLuaModules(bundleDatabase)
        val index = luaModules.getIndex()

        assertTrue("test-bundle" in index.modules)
        assertTrue("test-bundle.common.shared" in index.modules)
        assertTrue("test-bundle.client.init" in index.modules)
        assertTrue("test-bundle.server.secret" !in index.modules)
        assertEquals("test-bundle.client.init", index.entrypoints.single().module)
    }

    @Test
    fun `preloads can expose custom module names`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle(
            BundleManifest(
                name = "test-bundle",
                preloads = mapOf("custom.module" to "client/custom.lua")
            )
        ) {
            writeText("client/custom.lua", "return 'custom'")
        }
        bundleDatabase.addBundle(bundle)

        val luaModules = createLuaModules(bundleDatabase)
        val module = assertNotNull(luaModules.getModule("custom.module"))

        assertEquals("test-bundle", module.bundle)
        assertEquals("client/custom.lua", module.path)
        assertEquals("return 'custom'", module.source)
    }

    @Test
    fun `later preload modules override earlier preload modules`() {
        val bundleDatabase = BundleDatabase()
        val firstBundle = createBundle(
            BundleManifest(name = "first", preloads = mapOf("custom.module" to "client/custom.lua"))
        ) {
            writeText("client/custom.lua", "return 'first'")
        }
        val secondBundle = createBundle(
            BundleManifest(name = "second", preloads = mapOf("custom.module" to "client/custom.lua"))
        ) {
            writeText("client/custom.lua", "return 'second'")
        }
        bundleDatabase.addBundle(firstBundle)
        bundleDatabase.addBundle(secondBundle)

        val luaModules = createLuaModules(bundleDatabase)
        val module = assertNotNull(luaModules.getModule("custom.module"))

        assertEquals("second", module.bundle)
        assertEquals("return 'second'", module.source)
    }

    @Test
    fun `server entrypoints are excluded`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle(
            BundleManifest(
                name = "test-bundle",
                entrypoints = listOf("server/init.lua")
            )
        ) {
            writeText("server/init.lua", "return 'server'")
        }
        bundleDatabase.addBundle(bundle)

        val luaModules = createLuaModules(bundleDatabase)

        assertTrue(luaModules.getIndex().entrypoints.isEmpty())
        assertNull(luaModules.getModule("test-bundle.server.init"))
    }

    private fun createLuaModules(bundleDatabase: BundleDatabase): ClientLuaModules {
        return ClientLuaModules(
            bundleDatabase,
            ClientBundleCache(ServerConfig(), LoggerFactory.getLogger(ClientLuaModulesTest::class.java))
        )
    }

    private fun createBundle(manifest: BundleManifest, block: TestBundleBuilder.() -> Unit): Bundle {
        val root = Files.createTempDirectory("client-lua-modules-${manifest.name}").toFile()
        TestBundleBuilder(root.toPath()).block()
        return Bundle(manifest, root)
    }

    private class TestBundleBuilder(private val root: Path) {
        fun writeText(relativePath: String, text: String) {
            val path = root.resolve(relativePath)
            path.parent?.createDirectories()
            path.writeText(text)
        }
    }
}
