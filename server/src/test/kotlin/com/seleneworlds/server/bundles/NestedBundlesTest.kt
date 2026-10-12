package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.ResourcesApi
import com.seleneworlds.server.config.ServerConfig
import kotlinx.serialization.json.Json
import org.slf4j.helpers.NOPLogger
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.*

class NestedBundlesTest {
    @Test
    fun `nested bundles keep flat identities and independent contents`() {
        val root = Files.createTempDirectory("nested-bundles").toFile()
        try {
            val parent = root.resolve("parent").apply { mkdirs() }
            parent.resolve("bundle.json").writeText("""{"name":"parent","dependencies":["npcs"]}""")
            parent.resolve("common").mkdirs()
            parent.resolve("common/own.lua").writeText("return 1")
            parent.resolve("init.lua").writeText("return 'init'")
            parent.resolve("server").mkdirs()
            parent.resolve("server/private.lua").writeText("return 'server'")
            parent.resolve("unrelated").mkdirs()
            parent.resolve("unrelated/ignored.lua").writeText("return 'ignored'")
            parent.resolve("README.md").writeText("documentation")
            // A manifest inside content is data, not another bundle.
            parent.resolve("common/bundle.json").writeText("""{"name":"not-a-bundle"}""")
            val child = parent.resolve("different-directory-name").apply { mkdirs() }
            child.resolve("common").mkdirs()
            child.resolve("bundle.json").writeText("""{"name":"npcs"}""")
            child.resolve("common/own.lua").writeText("return 2")
            val locator = ServerBundleLocator(Json, ServerConfig(bundlesPath = root.path), NOPLogger.NOP_LOGGER)
            val parentBundle = assertNotNull(locator.locateBundle("parent"))
            val childBundle = assertNotNull(locator.locateBundle("npcs"))
            assertEquals(child, childBundle.dir)
            assertNull(locator.locateBundle("parent/npcs"))
            assertNull(locator.locateBundle("not-a-bundle"))
            val database = BundleDatabase().apply { addBundle(parentBundle); addBundle(childBundle) }
            val resources = ResourcesApi(database)
            assertEquals(setOf("parent/init.lua", "parent/common/own.lua", "parent/server/private.lua"), resources.listFiles("parent", "*.lua").toSet())
            assertEquals(listOf("npcs/common/own.lua"), resources.listFiles("npcs", "*.lua"))
            assertEquals("return 2", resources.loadAsString("npcs/common/own.lua"))
            assertFalse(resources.fileExists("parent/different-directory-name/common/own.lua"))
            assertTrue(resources.listFiles("parent", "different-directory-name/*.lua").isEmpty())
            val cache = ClientBundleCache(ServerConfig(bundlesPath = root.path), NOPLogger.NOP_LOGGER)
            val oldHash = cache.getHash(parent)
            child.resolve("common/own.lua").writeText("return 3")
            parent.resolve("README.md").writeText("changed documentation")
            parent.resolve("unrelated/ignored.lua").writeText("changed unrelated file")
            assertFalse(resources.fileExists("parent/README.md"))
            assertFalse(resources.fileExists("parent/common/../unrelated/ignored.lua"))
            val freshCache = ClientBundleCache(ServerConfig(bundlesPath = root.path), NOPLogger.NOP_LOGGER)
            assertEquals(oldHash, freshCache.getHash(parent))
            parent.resolve("server/private.lua").writeText("changed server code")
            assertNotEquals(oldHash, ClientBundleCache(ServerConfig(), NOPLogger.NOP_LOGGER).getHash(parent))
            val zip = root.resolve("parent.zip")
            cache.createClientZip(parent, zip)
            ZipFile(zip).use { archive ->
                assertEquals(setOf("bundle.json", "init.lua", "common/own.lua", "common/bundle.json"), archive.entries().asSequence().map { it.name }.toSet())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `duplicate flat names are rejected across nesting levels`() {
        val root = Files.createTempDirectory("duplicate-bundles").toFile()
        try {
            for (path in listOf("first", "parent/second")) {
                root.resolve(path).apply { mkdirs() }.resolve("bundle.json").writeText("""{"name":"same"}""")
            }
            val locator = ServerBundleLocator(Json, ServerConfig(bundlesPath = root.path), NOPLogger.NOP_LOGGER)
            assertFailsWith<IllegalArgumentException> { locator.locateBundle("same") }
        } finally {
            root.deleteRecursively()
        }
    }
}
