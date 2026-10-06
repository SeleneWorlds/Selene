package com.seleneworlds.common.resources

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.ResourcesApi
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ResourcesApiTest {
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
        } finally {
            root.toFile().deleteRecursively()
            outside.toFile().delete()
        }
    }
}
