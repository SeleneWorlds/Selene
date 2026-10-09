package com.seleneworlds.server.config

import java.nio.file.Files
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.sksamuel.hoplite.ConfigLoaderBuilder

class ServerConfigTest {
    @Test
    fun `loads packet rate limits from server properties`() {
        val file = Files.createTempFile("packet-rate-limits", ".properties").toFile()
        try {
            file.writeText("""
                packet_rate_limits.RequestMovePacket.packets_per_second=20
                packet_rate_limits.RequestMovePacket.burst=10
                packet_rate_limits.PreferencesPacket.packets_per_second=0.5
                packet_rate_limits.PreferencesPacket.burst=2
            """.trimIndent())
            val config = ConfigLoaderBuilder.default().build().loadConfigOrThrow<ServerConfig>(file.absolutePath)
            assertEquals(mapOf(
                "RequestMovePacket" to PacketRateLimit(20.0, 10),
                "PreferencesPacket" to PacketRateLimit(0.5, 2)
            ), config.packetRateLimits)
        } finally {
            file.delete()
        }
    }

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
