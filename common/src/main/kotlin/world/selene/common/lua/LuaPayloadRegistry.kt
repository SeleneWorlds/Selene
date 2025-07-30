package world.selene.common.lua

import party.iroiro.luajava.value.LuaValue

class LuaPayloadRegistry {

    private val payloadHandlers = mutableMapOf<String, LuaValue>()

    fun registerHandler(payloadId: String, callback: LuaValue) {
        payloadHandlers[payloadId] = callback
    }

    fun retrieveHandler(payloadId: String): LuaValue? {
        return payloadHandlers[payloadId]
    }

}