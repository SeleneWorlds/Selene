package world.selene.client.network

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.ClosureTrace
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAnyMap
import world.selene.common.lua.util.xpCall

/**
 * Send and handle custom payloads.
 */
class NetworkLuaApi(private val api: NetworkApi) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", ::luaHandlePayload)
        table.register("SendToServer", ::luaSendToServer)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val function = lua.checkFunction(2)
        val trace = lua.getCallerInfo()
        val callback: (Map<*, *>) -> Unit = { payload ->
            val lua = function.state()
            function.push(lua)
            lua.push(payload)
            try {
                lua.xpCall(1, 0, ClosureTrace { "[payload \"$payloadId\"] registered in <$trace>" })
            } catch (e: LuaException) {
                logger.error("Lua Error in Payload Handler", e)
            }
        }
        api.handlePayload(payloadId, callback)
        return 0
    }

    private fun luaSendToServer(lua: Lua): Int {
        api.sendToServer(lua.checkString(1), lua.toAnyMap(2))
        return 0
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NetworkLuaApi::class.java)
    }
}
