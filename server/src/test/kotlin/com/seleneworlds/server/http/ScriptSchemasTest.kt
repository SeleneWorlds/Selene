package com.seleneworlds.server.http

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ScriptSchemasTest {
    @Test
    fun `large schemas return intact and errors restore the Lua stack`() {
        val manager = LuaManager(LuaPackageModule())
        val lua = manager.lua
        try {
            lua.load(LuaManager.loadBuffer("""
                function require(name)
                    if name == "selene.json" then return { encode = function(value) return value end } end
                    assert(name == "moonlight-editor.server.lua.editor")
                    return { scriptedSchema = function(module, callback)
                        assert(module == "test.provider")
                        if callback == "broken" then error("provider failed") end
                        assert(callback == "custom")
                        return string.rep("x", 100000)
                    end }
                end
            """.trimIndent()), "script-schema-test")
            lua.pCall(0, 0)
            val schemas = ScriptSchemas(manager)
            assertEquals("x".repeat(100000), schemas.fetch("test.provider", "custom"))
            assertEquals(0, lua.top)
            assertFails { schemas.fetch("test.provider", "broken") }
            assertEquals(0, lua.top)
            assertEquals(100000, schemas.fetch("test.provider", "custom").length)
        } finally {
            lua.close()
        }
    }
}
