package com.seleneworlds.server.sqlite

import com.seleneworlds.server.config.ServerConfig
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqliteApiTest {
    @Test
    fun `creates and queries a database file with prepared parameters`() {
        val saveDirectory = Files.createTempDirectory("selene-sqlite-test").toFile()
        val api = SqliteApi(ServerConfig(savePath = saveDirectory.absolutePath))
        val database = api.open("data/players.db")

        database.execute("CREATE TABLE players (name TEXT NOT NULL, score INTEGER NOT NULL)")
        assertEquals(1, database.execute("INSERT INTO players VALUES (?, ?)", listOf("Selene", 42)))

        assertEquals(
            listOf(mapOf("name" to "Selene", "score" to 42)),
            database.query("SELECT name, score FROM players WHERE score > ?", listOf(10))
        )
        assertEquals(1, database.scalar("SELECT COUNT(*) FROM players"))
        assertTrue(saveDirectory.resolve("data/players.db").isFile)

        database.close()
        assertFalse(database.isOpen)
        api.dispose()
    }

    @Test
    fun `disposing the api closes all open databases`() {
        val api = SqliteApi(ServerConfig())
        val first = api.open(":memory:")
        val second = api.open(":memory:")

        api.dispose()

        assertFalse(first.isOpen)
        assertFalse(second.isOpen)
    }

    @Test
    fun `rejects database paths outside the save directory`() {
        val saveDirectory = Files.createTempDirectory("selene-sqlite-test").toFile()
        val api = SqliteApi(ServerConfig(savePath = saveDirectory.absolutePath))

        assertFailsWith<IllegalArgumentException> { api.open("../outside.db") }
    }
}
