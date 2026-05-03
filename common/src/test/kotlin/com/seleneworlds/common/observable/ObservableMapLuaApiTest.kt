package com.seleneworlds.common.observable

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.libraries.LuaTablexModule
import com.seleneworlds.common.lua.util.newTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObservableMapLuaApiTest {

    @Test
    fun `assigning a list literal stores it as a Kotlin list`() {
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
                    data = tablex.observable()
                    data.items = { "a", "b" }
                    """.trimIndent()
                ),
                "observable_map_list_assignment_test"
            )
            luaManager.lua.pCall(0, 0)

            luaManager.lua.getGlobal("data")
            val observableMap = luaManager.lua.toJavaObject(-1) as ObservableMap
            luaManager.lua.pop(1)

            val list = observableMap["items"]
            assertIs<List<*>>(list)
            assertEquals(listOf("a", "b"), list)
        } finally {
            luaManager.lua.close()
        }
    }
}
