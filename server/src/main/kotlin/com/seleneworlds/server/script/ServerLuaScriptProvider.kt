package com.seleneworlds.server.script

import org.slf4j.Logger
import com.seleneworlds.server.profiling.ServerProfiler
import com.seleneworlds.common.lua.LuaManager

class ServerLuaScriptProvider(
    private val luaManager: LuaManager,
    private val logger: Logger,
    private val profiler: ServerProfiler
) : ServerScriptProvider {

    override fun loadEntityScript(module: String): ServerEntityScript {
        return ServerEntityLuaScript(module, luaManager, logger, profiler)
    }
}
