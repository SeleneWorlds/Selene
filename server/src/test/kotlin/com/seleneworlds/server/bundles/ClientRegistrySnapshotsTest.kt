package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.custom.CustomRegistry
import com.seleneworlds.common.data.custom.CustomRegistryDefinition
import com.seleneworlds.common.tiles.TileRegistry
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
import kotlin.test.assertNull
import kotlin.test.assertNotEquals

class ClientRegistrySnapshotsTest {
    @Test
    fun `snapshots include runtime additions and invalidate on edits and removals`() {
        val database = BundleDatabase()
        database.addBundle(createBundle("runtime") {
            writeJson("common/data/test/tiles/static.json", """{"visual":"test:static"}""")
            writeJson("common/data/test/tiles/client_override.json", """{"visual":"test:common"}""")
            writeJson("client/data/test/tiles/client_override.json", """{"visual":"test:client"}""")
            writeJson("common/data/test/registries/secrets.json", """{"name":"secrets","platform":"server"}""")
            writeJson("server/data/test/secrets/secret.json", """{"password":"private"}""")
        })
        val tiles = TileRegistry(seleneJson).also { it.load(database) }
        val secrets = CustomRegistry(seleneJson, CustomRegistryDefinition("secrets", "server"))
            .also { it.load(database) }
        val provider = object : RegistryProvider {
            override fun getRegistries(): Map<Identifier, Registry<*>> = mapOf(
                TileRegistry.IDENTIFIER to tiles,
                Identifier("test", "secrets") to secrets
            )
            override fun getRegistry(identifier: Identifier): Registry<*>? = getRegistries()[identifier]
        }
        val snapshots = ClientRegistrySnapshots(database,
            ClientBundleCache(ServerConfig(), LoggerFactory.getLogger(javaClass)), seleneJson, provider)
        val generated = Identifier("test", "generated")
        tiles.upsertEntry(generated, seleneJson.parseToJsonElement("""{"visual":"test:first"}"""))
        assertNull(tiles.getSourcePath(generated))
        val first = assertNotNull(snapshots.getRegistry(TileRegistry.IDENTIFIER))
        assertEquals("test:first", first.entries.getValue("test:generated").jsonObject.getValue("visual").jsonPrimitive.content)
        assertEquals("test:client", first.entries.getValue("test:client_override").jsonObject.getValue("visual").jsonPrimitive.content)
        assertNull(snapshots.getRegistry(Identifier("test", "secrets")))
        tiles.upsertEntry(generated, seleneJson.parseToJsonElement("""{"visual":"test:changed"}"""))
        val changed = assertNotNull(snapshots.getRegistry(TileRegistry.IDENTIFIER))
        assertNotEquals(first.hash, changed.hash)
        assertEquals("test:changed", changed.entries.getValue("test:generated").jsonObject.getValue("visual").jsonPrimitive.content)
        tiles.removeEntry(generated)
        // Deleting a loaded entry must also win over its unchanged file on disk.
        tiles.removeEntry(Identifier("test", "static"))
        val removed = assertNotNull(snapshots.getRegistry(TileRegistry.IDENTIFIER))
        assertEquals(setOf("test:client_override"), removed.entries.keys)
        assertNotEquals(changed.hash, removed.hash)
    }

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

    @Test
    fun `snapshots expose client and common messages but not server messages`() {
        val bundleDatabase = BundleDatabase()
        val bundle = createBundle("bundle") {
            writeText("common/i18n/messages_en.properties", "greeting=Hello\nshared=Common")
            writeText("client/i18n/ui_de_DE.properties", "greeting=Hallo\nshared=Client")
            writeText("server/i18n/messages_en.properties", "secret=hidden")
        }
        bundleDatabase.addBundle(bundle)

        val messages = assertNotNull(
            createSnapshots(bundleDatabase).getRegistry(Identifier.withDefaultNamespace("messages"))
        ).entries.getValue("selene:messages").jsonObject

        assertEquals("Hello", messages.getValue("greeting").jsonObject.getValue("en").jsonPrimitive.content)
        assertEquals("Hallo", messages.getValue("greeting").jsonObject.getValue("de-DE").jsonPrimitive.content)
        assertEquals("Client", messages.getValue("shared").jsonObject.getValue("de-DE").jsonPrimitive.content)
        assertTrue("secret" !in messages)
    }

    @Test
    fun `web message snapshots preserve UTF-8 umlauts and properties escapes`() {
        val bundleDatabase = BundleDatabase()
        bundleDatabase.addBundle(createBundle("utf8") {
            writeText("client/i18n/messages_de.properties", "menu.title=Menü\ncharacters=ÄÖÜ äöü ß\nescaped=Men\\u00fc\n")
            writeText("common/i18n/messages_de.properties", "common=Zurück\n")
        })

        val snapshot = assertNotNull(
            createSnapshots(bundleDatabase).getRegistry(Identifier.withDefaultNamespace("messages"))
        )
        // Check the JSON payload consumed by the web client, including serialization.
        val messages = seleneJson.parseToJsonElement(
            snapshot.entries.getValue("selene:messages").toString()
        ).jsonObject

        assertEquals("Menü", messages.getValue("menu.title").jsonObject.getValue("de").jsonPrimitive.content)
        assertEquals("ÄÖÜ äöü ß", messages.getValue("characters").jsonObject.getValue("de").jsonPrimitive.content)
        assertEquals("Menü", messages.getValue("escaped").jsonObject.getValue("de").jsonPrimitive.content)
        assertEquals("Zurück", messages.getValue("common").jsonObject.getValue("de").jsonPrimitive.content)
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
        fun writeText(relativePath: String, text: String) {
            val path = root.resolve(relativePath)
            path.parent.createDirectories()
            path.writeText(text)
        }
    }
}
