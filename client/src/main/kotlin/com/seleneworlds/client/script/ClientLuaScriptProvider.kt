package com.seleneworlds.client.script

import org.slf4j.Logger
import com.seleneworlds.common.lua.LuaManager

class ClientLuaScriptProvider(
    private val luaManager: LuaManager,
    private val logger: Logger
) : ClientScriptProvider {

    override fun loadEntityScript(module: String): ClientEntityScript {
        return ClientEntityLuaScript(module, luaManager, logger)
    }

}
