package com.seleneworlds.common.resources

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.ResourcesApi
import com.seleneworlds.common.data.custom.CustomRegistry
import com.seleneworlds.common.data.custom.CustomRegistryDefinition
import com.seleneworlds.common.serialization.seleneJson
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ResourcesApiTest {
    @Test
    fun `registry file listing scopes platform and registry while including all namespaces`() {
        val root = Files.createTempDirectory("resources-api-registry")
        try {
            val entries = listOf(
                "common/data/example/items/first.json",
                "common/data/other/items/nested/second.json"
            )
            for (path in entries + listOf(
                "common/data/example/items.json",
                "common/data/example/items/notes.txt",
                "common/data/example/monsters/other.json",
                "server/data/example/items/server.json",
                "client/assets/unrelated.json"
            )) {
                val file = root.resolve(path)
                Files.createDirectories(file.parent)
                Files.writeString(file, "{}")
            }
            val resources = ResourcesApi(BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
            })
            val registry = CustomRegistry(seleneJson, CustomRegistryDefinition("items", "common"))
            assertEquals(entries.map { "active/$it" }, resources.listRegistryFiles("active", registry).sorted())
            assertEquals(emptyList(), resources.listRegistryFiles("unknown", registry))
            Files.delete(root.resolve(entries.first()))
            assertEquals(listOf("active/${entries.last()}"), resources.listRegistryFiles("active", registry))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `file listing matches nested entries and reflects filesystem changes`() {
        val root = Files.createTempDirectory("resources-api-list")
        try {
            val files = listOf(
                "common/data/example/items/first.json",
                "common/data/example/items/nested/second.json",
                "common/data/example/monsters/other.json",
                "client/assets/unrelated.json",
                "server/init.lua"
            )
            for (path in files) {
                val file = root.resolve(path)
                Files.createDirectories(file.parent)
                Files.writeString(file, "{}")
            }
            val resources = ResourcesApi(BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
            })
            val filter = "common/data/*/items/*.json"
            assertEquals(files.take(2).map { "active/$it" }.sorted(), resources.listFiles("active", filter).sorted())
            assertEquals(listOf("active/server/init.lua"), resources.listFiles("active", "*.lua"))
            assertEquals(emptyList(), resources.listFiles("active", "missing/data/*.json"))
            Files.delete(root.resolve(files.first()))
            Files.writeString(root.resolve("common/data/example/items/new.json"), "{}")
            assertEquals(listOf(
                "active/common/data/example/items/nested/second.json",
                "active/common/data/example/items/new.json"
            ), resources.listFiles("active", filter).sorted())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `creates resources without overwriting existing files`() {
        val root = Files.createTempDirectory("resources-api-create")
        try {
            Files.createDirectories(root.resolve("server/data/example/items"))
            val resources = ResourcesApi(BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
            })
            val path = "active/server/data/example/items/new_entry.json"

            resources.createAsString(path, "{}\n")
            assertEquals("{}\n", resources.loadAsString(path))
            assertFailsWith<java.nio.file.FileAlreadyExistsException> {
                resources.createAsString(path, "overwritten")
            }
            assertEquals("{}\n", resources.loadAsString(path))
            assertFailsWith<IllegalArgumentException> {
                resources.createAsString("active/missing/entry.json", "{}")
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `lists enabled bundles and reads and writes existing files`() {
        val root = Files.createTempDirectory("resources-api")
        try {
            val file = root.resolve("server/data/example.json")
            Files.createDirectories(file.parent)
            Files.writeString(file, "before")
            val database = BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
                addBundle(Bundle(BundleManifest("inactive"), root.toFile()), enabled = false)
            }
            val resources = ResourcesApi(database)

            assertEquals(listOf("active"), resources.listBundles())
            assertEquals("before", resources.loadAsString("active/server/data/example.json"))
            resources.saveAsString("active/server/data/example.json", "after")
            assertEquals("after", Files.readString(file))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects paths outside a bundle`() {
        val root = Files.createTempDirectory("resources-api-root")
        val outside = Files.createTempFile("resources-api-outside", ".txt")
        try {
            val resources = ResourcesApi(BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
            })
            val path = "active/../${outside.fileName}"

            assertFalse(resources.fileExists(path))
            assertFailsWith<IllegalArgumentException> { resources.loadAsString(path) }
            assertFailsWith<IllegalArgumentException> { resources.saveAsString(path, "changed") }
            assertFailsWith<IllegalArgumentException> { resources.createAsString(path, "changed") }
        } finally {
            root.toFile().deleteRecursively()
            outside.toFile().delete()
        }
    }
}
