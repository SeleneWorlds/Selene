package com.seleneworlds.common.data.json

import kotlinx.serialization.Serializable
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.RegistriesApi
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.serialization.seleneJson
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.io.path.readText
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalPathApi::class)
class FileBasedRegistryTest {

    @Test
    fun dirtyStateFollowsRuntimeDivergenceAndReverts() {
        withBundleDatabase { database, root ->
            val identifier = Identifier("test", "entry")
            val path = "${root.fileName}/common/data/test/widgets/entry.json"
            writeFile(root.resolve("common/data/test/widgets/entry.json"), """{"value":"original"}""")
            val registry = TestRegistry()
            registry.load(database)
            assertFalse(registry.isDirty(identifier))
            assertEquals(emptyMap(), registry.getDirtyEntries())

            assertNotNull(registry.get(identifier)).value = "changed in place"
            assertEquals(mapOf(identifier to path), registry.getDirtyEntries())
            assertNotNull(registry.get(identifier)).value = "original"
            assertFalse(registry.isDirty(identifier))

            registry.upsertEntry(identifier, seleneJson.parseToJsonElement("""{"value":"replacement"}"""))
            assertEquals(mapOf(identifier to path), registry.getDirtyEntries())
            registry.upsertEntry(identifier, seleneJson.parseToJsonElement("""{"value":"original"}"""))
            assertEquals(emptyMap(), registry.getDirtyEntries())
        }
    }

    @Test
    fun loadedPersistedAndHotReloadedEntriesAreClean() {
        withBundleDatabase { database, root ->
            val identifier = Identifier("test", "entry")
            val relative = "common/data/test/widgets/entry.json"
            val file = writeFile(root.resolve(relative), """{"value":"original"}""")
            val registry = TestRegistry()
            registry.load(database)
            registry.upsertEntry(identifier, seleneJson.parseToJsonElement("""{"value":"runtime"}"""))
            file.writeText("""{"value":"runtime"}""")
            registry.markPersisted(identifier)
            assertFalse(registry.isDirty(identifier))

            assertNotNull(registry.get(identifier)).value = "dirty"
            file.writeText("""{"value":"hot reload"}""")
            registry.bundleFileUpdated(database, assertNotNull(database.getBundle(root.fileName.toString())), relative)
            assertFalse(registry.isDirty(identifier))
            assertEquals("hot reload", registry.get(identifier)?.value)

            assertNotNull(registry.get(identifier)).value = "dirty again"
            registry.load(database)
            assertEquals(emptyMap(), registry.getDirtyEntries())
        }
    }

    @Test
    fun newEntriesAndRuntimeRemovalsTrackDivergence() {
        withBundleDatabase { database, root ->
            val existing = Identifier("test", "existing")
            val relative = "common/data/test/widgets/existing.json"
            val file = writeFile(root.resolve(relative), """{"value":"existing"}""")
            val registry = TestRegistry()
            registry.load(database)
            registry.removeEntry(existing)
            assertEquals(mapOf(existing to "${root.fileName}/$relative"), registry.getDirtyEntries())
            registry.markPersisted(existing)
            assertEquals(emptyMap(), registry.getDirtyEntries())

            registry.load(database)
            file.toFile().delete()
            registry.bundleFileRemoved(database, assertNotNull(database.getBundle(root.fileName.toString())), relative)
            assertEquals(emptyMap(), registry.getDirtyEntries())

            val created = Identifier("test", "new")
            val path = "${root.fileName}/common/data/test/widgets/new.json"
            registry.upsertEntry(created, seleneJson.parseToJsonElement("""{"value":"new"}"""), sourcePath = path)
            assertEquals(mapOf(created to path), registry.getDirtyEntries())
            registry.removeEntry(created)
            assertEquals(emptyMap(), registry.getDirtyEntries())
        }
    }

    @Test
    fun serializationReadsCurrentRuntimeEntry() {
        val registry = TestRegistry()
        val identifier = Identifier("test", "entry")
        registry.upsertEntry(identifier, seleneJson.parseToJsonElement("""{"value":"applied"}"""))
        assertNotNull(registry.get(identifier)).value = "mutated"

        assertEquals(seleneJson.parseToJsonElement("""{"value":"mutated"}"""), registry.getEntryElement(identifier))
        registry.removeEntry(identifier)
        assertNull(registry.getEntryElement(identifier))
    }

    @Test
    fun runtimeEntriesCanHaveSourcePathsBeforePersistence() {
        withBundleDatabase { _, bundleRoot ->
            val registry = TestRegistry()
            val api = RegistriesApi(object : RegistryProvider {
                override fun getRegistries(): Map<Identifier, Registry<*>> = mapOf(Identifier("selene", "widgets") to registry)
                override fun getRegistry(identifier: Identifier): Registry<*> = registry
            })
            val identifier = Identifier("test", "new_entry")
            val relativePath = "common/data/test/widgets/new_entry.json"
            val sourcePath = "${bundleRoot.fileName}/$relativePath"
            var notifiedPath: String? = null
            registry.addReloadListener(object : RegistryReloadListener<TestEntry> {
                override fun onEntryAdded(registry: Registry<TestEntry>, identifier: Identifier, newData: TestEntry) {
                    notifiedPath = registry.getSourcePath(identifier)
                }
            })

            api.add("widgets", identifier.toString(), mapOf("value" to "created"), sourcePath)

            assertEquals(sourcePath, registry.getSourcePath(identifier))
            assertEquals(sourcePath, notifiedPath)
            assertFalse(bundleRoot.resolve(relativePath).exists())

            api.add("widgets", identifier.toString(), mapOf("value" to "updated"))
            assertEquals(sourcePath, registry.getSourcePath(identifier))
            assertFalse(bundleRoot.resolve(relativePath).exists())

            api.remove("widgets", identifier.toString())
            assertNull(registry.getSourcePath(identifier))
            assertNull(registry.get(identifier))
        }
    }

    @Test
    fun runtimeUpdatesPreserveSourcePathWithoutWritingFile() {
        withBundleDatabase { bundleDatabase, bundleRoot ->
            val identifier = Identifier("test", "entry")
            val original = """{ "value": "original" }"""
            val path = writeFile(bundleRoot.resolve("common/data/test/widgets/entry.json"), original)
            val registry = TestRegistry()
            registry.load(bundleDatabase)
            val sourcePath = assertNotNull(registry.getSourcePath(identifier))

            registry.upsertEntry(identifier, seleneJson.parseToJsonElement("""{ "value": "updated" }"""))

            assertEquals("updated", registry.get(identifier)?.value)
            assertEquals(sourcePath, registry.getSourcePath(identifier))
            assertEquals(original, path.readText())

            val runtimeIdentifier = Identifier("test", "runtime")
            registry.upsertEntry(runtimeIdentifier, seleneJson.parseToJsonElement(original))
            assertNull(registry.getSourcePath(runtimeIdentifier))

            registry.removeEntry(runtimeIdentifier)
            assertNull(registry.get(runtimeIdentifier))
            assertEquals("updated", registry.get(identifier)?.value)
            assertEquals(original, path.readText())
        }
    }

    @Test
    fun prefersMergedRegistryFileOverDirectoryWalk() {
        withBundleDatabase { bundleDatabase, bundleRoot ->
            writeFile(
                bundleRoot.resolve("common/data/test/widgets.json"),
                """
                {
                  "entries": {
                    "entry": { "value": "merged" },
                    "nested/item": { "value": "nested" }
                  }
                }
                """.trimIndent()
            )
            writeFile(
                bundleRoot.resolve("common/data/test/widgets/entry.json"),
                """
                { "value": "directory" }
                """.trimIndent()
            )

            val registry = TestRegistry()
            registry.load(bundleDatabase)

            assertEquals("merged", registry.get(Identifier("test", "entry"))?.value)
            assertEquals("nested", registry.get(Identifier("test", "nested/item"))?.value)
            assertEquals(
                "${bundleRoot.fileName}/common/data/test/widgets.json",
                registry.getSourcePath(Identifier("test", "entry"))
            )
        }
    }

    @Test
    fun laterBundlesOverrideEarlierWhenUsingMergedFiles() {
        val firstRoot = createTempDirectory("registry-bundle-first")
        val secondRoot = createTempDirectory("registry-bundle-second")
        try {
            writeFile(
                firstRoot.resolve("common/data/test/widgets.json"),
                """
                {
                  "entries": {
                    "entry": { "value": "first" }
                  }
                }
                """.trimIndent()
            )
            writeFile(
                secondRoot.resolve("common/data/test/widgets.json"),
                """
                {
                  "entries": {
                    "entry": { "value": "second" }
                  }
                }
                """.trimIndent()
            )

            val bundleDatabase = BundleDatabase().apply {
                addBundle(Bundle(BundleManifest(name = "first"), firstRoot.toFile()))
                addBundle(Bundle(BundleManifest(name = "second"), secondRoot.toFile()))
            }

            val registry = TestRegistry()
            registry.load(bundleDatabase)

            assertEquals("second", registry.get(Identifier("test", "entry"))?.value)
            assertEquals(
                "second/common/data/test/widgets.json",
                registry.getSourcePath(Identifier("test", "entry"))
            )
        } finally {
            firstRoot.deleteRecursively()
            secondRoot.deleteRecursively()
        }
    }

    @Test
    fun fallsBackToDirectoryEntriesWhenMergedFileRemoved() {
        withBundleDatabase { bundleDatabase, bundleRoot ->
            writeFile(
                bundleRoot.resolve("common/data/test/widgets.json"),
                """
                {
                  "entries": {
                    "entry": { "value": "merged" }
                  }
                }
                """.trimIndent()
            )
            writeFile(
                bundleRoot.resolve("common/data/test/widgets/entry.json"),
                """
                { "value": "directory" }
                """.trimIndent()
            )

            val registry = TestRegistry()
            registry.load(bundleDatabase)
            assertEquals("merged", registry.get(Identifier("test", "entry"))?.value)

            bundleRoot.resolve("common/data/test/widgets.json").toFile().delete()
            val bundle = assertNotNull(bundleDatabase.getBundle(bundleRoot.fileName.toString()))
            registry.bundleFileRemoved(bundleDatabase, bundle, "common/data/test/widgets.json")

            assertEquals("directory", registry.get(Identifier("test", "entry"))?.value)
        }
    }

    @Test
    fun ignoresPerEntryHotReloadWhileMergedFileExists() {
        withBundleDatabase { bundleDatabase, bundleRoot ->
            writeFile(
                bundleRoot.resolve("common/data/test/widgets.json"),
                """
                {
                  "entries": {
                    "entry": { "value": "merged" }
                  }
                }
                """.trimIndent()
            )
            val entryFile = writeFile(
                bundleRoot.resolve("common/data/test/widgets/entry.json"),
                """
                { "value": "directory" }
                """.trimIndent()
            )

            val registry = TestRegistry()
            registry.load(bundleDatabase)

            entryFile.writeText("""{ "value": "updated-directory" }""")
            val bundle = assertNotNull(bundleDatabase.getBundle(bundleRoot.fileName.toString()))
            registry.bundleFileUpdated(bundleDatabase, bundle, "common/data/test/widgets/entry.json")

            assertEquals("merged", registry.get(Identifier("test", "entry"))?.value)
        }
    }

    private fun withBundleDatabase(block: (BundleDatabase, Path) -> Unit) {
        val rootDir = createTempDirectory("registry-bundle")
        try {
            val bundle = Bundle(BundleManifest(name = rootDir.fileName.toString()), rootDir.toFile())
            val bundleDatabase = BundleDatabase().apply { addBundle(bundle) }
            block(bundleDatabase, rootDir)
        } finally {
            rootDir.deleteRecursively()
        }
    }

    private fun writeFile(path: Path, content: String): Path {
        path.parent.createDirectories()
        path.writeText(content)
        return path
    }

    private class TestRegistry : FileBasedRegistry<TestEntry>(
        seleneJson,
        "common",
        "widgets",
        TestEntry::class,
        TestEntry.serializer()
    )

    @Serializable
    private data class TestEntry(var value: String)
}
