package com.seleneworlds.server.sqlite

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.server.config.ServerConfig
import party.iroiro.luajava.Lua
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqliteLuaApiTest {
    @Test
    fun `lua can execute prepared statements and query rows`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val sqliteApi = SqliteApi(ServerConfig())
        val module = SqliteLuaApi(sqliteApi)
        try {
            module.initialize(luaManager)
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable { module.register(this) })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal("SQLite")
            luaManager.lua.setField(-2, module.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local sqlite = SQLite
                    local db = sqlite.open(":memory:")
                    db:execute("CREATE TABLE messages (text TEXT, priority INTEGER)")
                    inserted = db:execute("INSERT INTO messages VALUES (?, ?)", { "hello", 7 })
                    local rows = db:query("SELECT text, priority FROM messages WHERE priority = ?", 7)
                    resultText = rows[1].text
                    resultPriority = rows[1].priority
                    count = db:scalar("SELECT COUNT(*) FROM messages")
                    wasOpen = db.isOpen
                    db:close()
                    isOpenAfterClose = db.isOpen
                    """.trimIndent()
                ),
                "sqlite_lua_api_test"
            )
            luaManager.lua.pCall(0, 0)

            assertEquals(1, luaManager.lua.globalInt("inserted"))
            assertEquals("hello", luaManager.lua.globalString("resultText"))
            assertEquals(7, luaManager.lua.globalInt("resultPriority"))
            assertEquals(1, luaManager.lua.globalInt("count"))
            assertTrue(luaManager.lua.globalBoolean("wasOpen"))
            assertFalse(luaManager.lua.globalBoolean("isOpenAfterClose"))
        } finally {
            sqliteApi.dispose()
            luaManager.lua.close()
        }
    }

    private fun Lua.globalInt(name: String): Int {
        getGlobal(name)
        return toInteger(-1).toInt().also { pop(1) }
    }

    private fun Lua.globalString(name: String): String {
        getGlobal(name)
        return checkNotNull(toString(-1)).also { pop(1) }
    }

    private fun Lua.globalBoolean(name: String): Boolean {
        getGlobal(name)
        return toBoolean(-1).also { pop(1) }
    }
}
