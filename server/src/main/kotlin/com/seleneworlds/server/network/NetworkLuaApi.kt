package com.seleneworlds.server.network

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.script.ClosureTrace
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toSerializedMap
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.server.entities.EntityApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi

/**
 * Send and handle custom payloads.
 */
class NetworkLuaApi(private val api: NetworkApi) : LuaModule {

    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", ::luaHandlePayload)
        table.register("SendToPlayer", ::luaSendToPlayer)
        table.register("SendToPlayers", ::luaSendToPlayers)
        table.register("SendToEntity", ::luaSendToEntity)
        table.register("SendToEntities", ::luaSendToEntities)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val function = lua.checkFunction(2)
        val trace = lua.getCallerInfo()
        val callback: (Player, Map<*, *>) -> Unit = { player, payload ->
            val lua = function.state()
            function.push(lua)
            lua.push(player.api, Lua.Conversion.NONE)
            lua.push(payload)
            try {
                lua.xpCall(2, 0, ClosureTrace { "[payload \"$payloadId\"] registered in <$trace>" })
            } catch (e: LuaException) {
                logger.error("Lua Error in Payload Handler", e)
            }
        }
        api.handlePayload(payloadId, callback)
        return 0
    }

    private fun luaSendToPlayer(lua: Lua): Int {
        api.sendToPlayer(lua.checkUserdata<PlayerApi>(1), lua.checkString(2), lua.toSerializedMap(3) ?: emptyMap())
        return 0
    }

    private fun luaSendToPlayers(lua: Lua): Int {
        val players = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of players"))
        api.sendToPlayers(players, lua.checkString(2), lua.toSerializedMap(3) ?: emptyMap())
        return 0
    }

    private fun luaSendToEntity(lua: Lua): Int {
        api.sendToEntity(lua.checkUserdata<EntityApi>(1), lua.checkString(2), lua.toSerializedMap(3) ?: emptyMap())
        return 0
    }

    private fun luaSendToEntities(lua: Lua): Int {
        val entities = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of entities"))
        api.sendToEntities(entities, lua.checkString(2), lua.toSerializedMap(3) ?: emptyMap())
        return 0
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NetworkLuaApi::class.java)
    }
}
