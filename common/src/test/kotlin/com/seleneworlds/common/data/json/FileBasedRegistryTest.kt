package com.seleneworlds.common.data.json

import kotlinx.serialization.Serializable
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.serialization.seleneJson
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalPathApi::class)
class FileBasedRegistryTest {

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
    private data class TestEntry(val value: String)
}
