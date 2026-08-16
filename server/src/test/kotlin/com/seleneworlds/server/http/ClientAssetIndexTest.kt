package com.seleneworlds.server.http

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleManifest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClientAssetIndexTest {
    @Test
    fun `manifest maps logical asset paths to hashed client URLs without renaming files`() {
        val bundle = createBundle("base") {
            writeText("client/textures/terrain.png", "terrain-a")
            writeText("common/data/tile.json", """{"id":1}""")
        }

        val index = ClientAssetIndex.build(listOf(bundle))

        val terrainUrl = assertNotNull(index.manifest.assets["client/textures/terrain.png"])
        assertTrue(terrainUrl.matches(Regex("^/client/content/client/textures/terrain\\.[0-9a-f]{12}\\.png$")))
        assertTrue(bundle.dir.toPath().resolve("client/textures/terrain.png").toFile().exists())
        assertFalse(bundle.dir.toPath().resolve(terrainUrl.removePrefix("/client/content/")).toFile().exists())

        val hashedPath = terrainUrl.removePrefix("/client/content/")
        val asset = assertNotNull(index.resolveClient(hashedPath))
        assertEquals("client/textures/terrain.png", asset.logicalPath)
        assertEquals("terrain-a", asset.file.toPath().readText())
    }

    @Test
    fun `later bundles override earlier bundles in the client manifest`() {
        val base = createBundle("base") {
            writeText("client/textures/terrain.png", "base")
        }
        val override = createBundle("override") {
            writeText("client/textures/terrain.png", "override")
        }

        val index = ClientAssetIndex.build(listOf(base, override))

        val hashedPath = assertNotNull(index.manifest.assets["client/textures/terrain.png"])
            .removePrefix("/client/content/")
        val asset = assertNotNull(index.resolveClient(hashedPath))
        assertEquals("override", asset.file.toPath().readText())
    }

    @Test
    fun `bundle-specific hashed URLs resolve to assets from the requested bundle`() {
        val base = createBundle("base") {
            writeText("client/textures/terrain.png", "base")
        }
        val override = createBundle("override") {
            writeText("client/textures/terrain.png", "override")
        }

        val index = ClientAssetIndex.build(listOf(base, override))
        val baseManifest = assertNotNull(index.bundleManifest("base"))
        val overrideManifest = assertNotNull(index.bundleManifest("override"))
        val baseUrl = baseManifest.manifest.assets.getValue("client/textures/terrain.png")
        val overrideUrl = overrideManifest.manifest.assets.getValue("client/textures/terrain.png")
        assertTrue(baseUrl.startsWith("/bundles/base/content/"))
        assertTrue(overrideUrl.startsWith("/bundles/override/content/"))
        val baseHashedPath = baseUrl.removePrefix("/bundles/base/content/")
        val overrideHashedPath = overrideUrl.removePrefix("/bundles/override/content/")

        val baseAsset = assertNotNull(
            index.resolveBundle("base", baseHashedPath)
        )
        val overrideAsset = assertNotNull(
            index.resolveBundle("override", overrideHashedPath)
        )

        assertEquals("base", baseAsset.file.toPath().readText())
        assertEquals("override", overrideAsset.file.toPath().readText())
    }

    @Test
    fun `truncated hash collisions fail clearly`() {
        val bundle = createBundle("base") {
            writeText("client/a.txt", "0")
            writeText("client/b.txt", "26")
        }

        val error = assertFailsWith<IllegalStateException> {
            ClientAssetIndex.build(listOf(bundle), hashPrefixLength = 1)
        }

        assertTrue(error.message.orEmpty().contains("SHA-256 hash prefix collision"))
    }

    @Test
    fun `changed file content is not current for its old hashed URL`() {
        val bundle = createBundle("base") {
            writeText("client/textures/terrain.png", "terrain-a")
        }
        val index = ClientAssetIndex.build(listOf(bundle))
        val hashedPath = assertNotNull(index.manifest.assets["client/textures/terrain.png"])
            .removePrefix("/client/content/")
        val asset = assertNotNull(index.resolveClient(hashedPath))

        asset.file.writeText("terrain-b")

        assertFalse(index.isContentCurrent(asset))
    }

    private fun createBundle(name: String, block: TestBundleBuilder.() -> Unit): Bundle {
        val root = Files.createTempDirectory("client-asset-index-$name")
        TestBundleBuilder(root).block()
        return Bundle(BundleManifest(name = name), root.toFile())
    }

    private class TestBundleBuilder(private val root: Path) {
        fun writeText(relativePath: String, text: String) {
            val path = root.resolve(relativePath)
            path.parent?.createDirectories()
            path.writeText(text)
        }
    }
}
