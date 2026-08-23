package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.config.ClientConfig
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BundleUiStorageTest {
    @Test
    fun `persists values and isolates bundle entrypoints`() {
        val root = createTempDirectory("bundle-ui-storage-test-")
        val storage = BundleUiStorage(ClientConfig(browserUiStorageDir = root.toString()))

        assertNull(storage.load("world", "hud", "player"))
        storage.save("world", "hud", "player", "remembered map")

        assertEquals("remembered map", storage.load("world", "hud", "player"))
        assertNull(storage.load("world", "admin", "player"))
        assertNull(storage.load("another-world", "hud", "player"))
        assertEquals("remembered map", Files.readString(root.resolve("world/hud/player")))
    }

    @Test
    fun `rejects paths outside a bundle namespace`() {
        val storage = BundleUiStorage(ClientConfig(browserUiStorageDir = createTempDirectory().toString()))

        assertFailsWith<IllegalArgumentException> {
            storage.save("world", "hud", "../settings", "value")
        }
        assertFailsWith<IllegalArgumentException> {
            storage.load("../world", "hud", "player")
        }
    }
}
