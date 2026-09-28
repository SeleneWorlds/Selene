package com.seleneworlds.server.config

import java.nio.file.Files
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerConfigTest {
    @Test
    fun `resolves paths inside the save directory`() {
        val saveDirectory = Files.createTempDirectory("save-path-test").toFile()
        val config = ServerConfig(savePath = saveDirectory.absolutePath)

        assertEquals(
            saveDirectory.resolve("players/data.json").canonicalFile,
            config.resolveSavePath("players/data.json")
        )
    }

    @Test
    fun `rejects parent traversal and absolute paths`() {
        val saveDirectory = Files.createTempDirectory("save-path-test").toFile()
        val config = ServerConfig(savePath = saveDirectory.absolutePath)

        assertFailsWith<IllegalArgumentException> { config.resolveSavePath("../outside.json") }
        assertFailsWith<IllegalArgumentException> { config.resolveSavePath("/tmp/outside.json") }
    }

    @Test
    fun `rejects paths escaping through a symbolic link`() {
        val saveDirectory = Files.createTempDirectory("save-path-test").toFile()
        val outsideDirectory = Files.createTempDirectory("outside-save-path-test")
        saveDirectory.toPath().resolve("linked").createSymbolicLinkPointingTo(outsideDirectory)
        val config = ServerConfig(savePath = saveDirectory.absolutePath)

        assertFailsWith<IllegalArgumentException> { config.resolveSavePath("linked/outside.db") }
    }
}
