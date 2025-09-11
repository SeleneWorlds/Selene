package world.selene.common.network

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.util.CallerInfo

class LuaPayloadRegistry {

    data class PayloadHandler(
        val payloadId: String,
        val callback: LuaValue,
        val registrationSite: CallerInfo
    ) : LuaTrace {
        override fun luaTrace(): String {
            return "[payload \"$payloadId\"] registered in <$registrationSite>"
        }
    }

    private val payloadHandlers = mutableMapOf<String, PayloadHandler>()

    fun registerHandler(payloadId: String, callback: LuaValue, registrationSite: CallerInfo) {
        payloadHandlers[payloadId] = PayloadHandler(payloadId, callback, registrationSite)
    }

    fun retrieveHandler(payloadId: String): PayloadHandler? {
        return payloadHandlers[payloadId]
    }

}