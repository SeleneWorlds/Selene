package com.seleneworlds.common.lua.libraries

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.util.newTable
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LuaTablexModuleTest {

    @Test
    fun `deepEquals compares nested tables and observable maps by value`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val tablex = LuaTablexModule()
        try {
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable {
                tablex.register(this)
            })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal(tablex.name)
            luaManager.lua.setField(-2, tablex.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    local value = { name = "item", nested = { enabled = true }, values = { 1, 2, 3 } }
                    equalTables = tablex.deepEquals(value, { name = "item", nested = { enabled = true }, values = { 1, 2, 3 } })
                    equalObservable = tablex.deepEquals(tablex.observable(value), value)
                    unequalNestedValue = tablex.deepEquals(value, { name = "item", nested = { enabled = false }, values = { 1, 2, 3 } })
                    unequalMissingKey = tablex.deepEquals(value, { name = "item", nested = { enabled = true } })
                    """.trimIndent()
                ),
                "tablex_deep_equals_test"
            )
            luaManager.lua.pCall(0, 0)

            luaManager.lua.getGlobal("equalTables")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("equalObservable")
            assertTrue(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("unequalNestedValue")
            assertFalse(luaManager.lua.toBoolean(-1))
            luaManager.lua.pop(1)

            luaManager.lua.getGlobal("unequalMissingKey")
            assertFalse(luaManager.lua.toBoolean(-1))
        } finally {
            luaManager.lua.close()
        }
    }
}
