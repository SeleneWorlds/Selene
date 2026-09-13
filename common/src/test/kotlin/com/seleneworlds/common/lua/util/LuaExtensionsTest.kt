package com.seleneworlds.common.lua.util

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LuaExtensionsTest {

    @Test
    fun `locale conversion accepts underscore and hyphen separators`() {
        val luaManager = LuaManager(LuaPackageModule())
        try {
            luaManager.lua.push("en_US")
            assertEquals(Locale.US, luaManager.lua.toLocale(-1))
            luaManager.lua.pop(1)

            luaManager.lua.push("de-DE")
            assertEquals(Locale.GERMANY, luaManager.lua.toLocale(-1))
        } finally {
            luaManager.lua.close()
        }
    }
}
