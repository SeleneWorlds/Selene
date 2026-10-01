package com.seleneworlds.common.lua

import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import org.slf4j.LoggerFactory
import kotlin.test.Test

class LoggingLuaApiTest {
    @Test
    fun `lua can write every log level`() {
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val module = LoggingLuaApi(LoggerFactory.getLogger("LuaLoggingApiTest"))
        try {
            luaManager.lua.push(luaPackage.packageLoaded)
            luaManager.lua.push(luaManager.lua.newTable { module.register(this) })
            luaManager.lua.pushValue(-1)
            luaManager.lua.setGlobal("Logging")
            luaManager.lua.setField(-2, module.name)
            luaManager.lua.pop(1)

            luaManager.lua.load(
                LuaManager.loadBuffer(
                    """
                    Logging.trace("trace")
                    Logging.debug("debug")
                    Logging.info("info")
                    Logging.warn("warn")
                    Logging.error("error")
                    """.trimIndent()
                ),
                "logging_lua_api_test"
            )
            luaManager.lua.pCall(0, 0)
        } finally {
            luaManager.lua.close()
        }
    }
}
