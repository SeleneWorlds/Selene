package world.selene.common.lua

import party.iroiro.luajava.value.LuaValue

class LuaPayloadRegistry {

    data class PayloadHandler(
        val callback: LuaValue,
        val registrationSite: CallerInfo
    )

    private val payloadHandlers = mutableMapOf<String, PayloadHandler>()

    fun registerHandler(payloadId: String, callback: LuaValue, registrationSite: CallerInfo) {
        payloadHandlers[payloadId] = PayloadHandler(callback, registrationSite)
    }

    fun retrieveHandler(payloadId: String): PayloadHandler? {
        return payloadHandlers[payloadId]
    }

}