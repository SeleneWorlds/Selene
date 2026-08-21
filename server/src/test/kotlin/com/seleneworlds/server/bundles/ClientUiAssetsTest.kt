package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.BundleUiEntrypoint
import com.seleneworlds.server.config.ServerConfig
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClientUiAssetsTest {
    @Test
    fun `declared UI exposes its entrypoint and neighboring assets`() {
        val database = BundleDatabase()
        val root = Files.createTempDirectory("client-ui-assets")
        root.resolve("client/ui/hello").createDirectories()
        root.resolve("client/ui/hello/index.html").writeText("<h1>Hello</h1>")
        root.resolve("client/ui/hello/image.png").writeText("image")
        database.addBundle(Bundle(BundleManifest(
            name = "hello",
            ui = listOf(BundleUiEntrypoint("main", "client/ui/hello/index.html"))
        ), root.toFile()))

        val assets = ClientUiAssets(
            database,
            ClientBundleCache(ServerConfig(), LoggerFactory.getLogger(ClientUiAssetsTest::class.java))
        )

        val entrypoint = assets.getIndex().entrypoints.single()
        assertEquals("/client/ui/content/hello/main/index.html", entrypoint.url)
        assertNotNull(assets.resolve("hello", "main", "image.png"))
        assertNull(assets.resolve("hello", "main", "../outside.txt"))
    }
}
