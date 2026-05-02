package world.selene.client.script

import org.slf4j.Logger
import world.selene.common.lua.LuaManager

class ClientLuaScriptProvider(
    private val luaManager: LuaManager,
    private val logger: Logger
) : ClientScriptProvider {

    override fun loadEntityScript(module: String): ClientEntityScript {
        return ClientEntityLuaScript(module, luaManager, logger)
    }

}
