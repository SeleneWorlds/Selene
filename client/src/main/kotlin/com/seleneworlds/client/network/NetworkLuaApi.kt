package com.seleneworlds.client.network

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.script.ClosureTrace
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toSerializedMap
import com.seleneworlds.common.lua.util.xpCall

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
        api.sendToServer(lua.checkString(1), lua.toSerializedMap(2) ?: emptyMap())
        return 0
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NetworkLuaApi::class.java)
    }
}
