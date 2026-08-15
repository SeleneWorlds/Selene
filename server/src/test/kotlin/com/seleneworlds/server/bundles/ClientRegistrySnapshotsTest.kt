package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.server.config.ServerConfig
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClientRegistrySnapshotsTest {
    @Test
    fun `snapshots merge per-entry registry files in bundle order`() {
        val bundleDatabase = BundleDatabase()
        val firstBundle = createBundle("first") {
            writeJson("common/data/test/tiles/tile_1.json", """{"value":"first"}""")
            writeJson("common/data/test/tiles/tile_2.json", """{"value":"second"}""")
        }
        val secondBundle = createBundle("second") {
            writeJson("common/data/test/tiles/tile_1.json", """{"value":"override"}""")
        }
        bundleDatabase.addBundle(firstBundle)
        bundleDatabase.addBundle(secondBundle)

        val snapshots = createSnapshots(bundleDatabase)
        val tiles = assertNotNull(snapshots.getRegistry(Identifier.withDefaultNamespace("tiles")))

        assertEquals("override", tiles.entries.getValue("test:tile_1").jsonObject.getValue("value").jsonPrimitive.content)
        assertEquals("second", tiles.entries.getValue("test:tile_2").jsonObject.getValue("value").jsonPrimitive.content)
    }

    @Test
    fun `snapshots include merged registry files`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle("bundle") {
            writeJson(
                "common/data/test/sounds.json",
                """{"entries":{"sound_1":{"value":"sound"}}}"""
            )
        }
        bundleDatabase.addBundle(bundle)

        val snapshots = createSnapshots(bundleDatabase)
        val sounds = assertNotNull(snapshots.getRegistry(Identifier.withDefaultNamespace("sounds")))

        assertEquals("sound", sounds.entries.getValue("test:sound_1").jsonObject.getValue("value").jsonPrimitive.content)
    }

    @Test
    fun `custom client registries use definition identifiers`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle("bundle") {
            writeJson(
                "common/data/test/registries/widgets.json",
                """{"name":"widgets","platform":"client"}"""
            )
            writeJson("client/data/test/widgets/widget_1.json", """{"value":"widget"}""")
        }
        bundleDatabase.addBundle(bundle)

        val snapshots = createSnapshots(bundleDatabase)
        val index = snapshots.getIndex()
        val widgets = assertNotNull(snapshots.getRegistry(Identifier("test", "widgets")))

        assertTrue("test:widgets" in index.registries)
        assertEquals("widget", widgets.entries.getValue("test:widget_1").jsonObject.getValue("value").jsonPrimitive.content)
    }

    private fun createSnapshots(bundleDatabase: BundleDatabase): ClientRegistrySnapshots {
        return ClientRegistrySnapshots(
            bundleDatabase,
            ClientBundleCache(ServerConfig(), LoggerFactory.getLogger(ClientRegistrySnapshotsTest::class.java)),
            seleneJson
        )
    }

    private fun createBundle(name: String, block: TestBundleBuilder.() -> Unit): Bundle {
        val root = Files.createTempDirectory("client-registry-snapshots-$name").toFile()
        TestBundleBuilder(root.toPath()).block()
        return Bundle(BundleManifest(name = name), root)
    }

    private class TestBundleBuilder(private val root: Path) {
        fun writeJson(relativePath: String, json: String) {
            val path = root.resolve(relativePath)
            path.parent.createDirectories()
            path.writeText(json)
        }
    }
}
