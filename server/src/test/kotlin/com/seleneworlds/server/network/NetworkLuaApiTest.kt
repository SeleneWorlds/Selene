package com.seleneworlds.server.network

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.server.config.PacketRateLimit
import com.seleneworlds.server.players.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkLuaApiTest {
    @Test
    fun `lua registers payload defaults and rejects invalid limits`() {
        val manager = LuaManager(LuaPackageModule())
        val registry = PayloadRateLimitRegistry()
        val module = NetworkLuaApi(NetworkApi(PayloadHandlerRegistry<Player>(), seleneJson, registry))
        try {
            manager.lua.push(manager.lua.newTable { module.register(this) })
            manager.lua.setGlobal("Network")
            manager.lua.load(LuaManager.loadBuffer("""
                Network.setDefaultPayloadRateLimit("example:chat", 0.5, 3)
                assert(not pcall(Network.setDefaultPayloadRateLimit, "example:bad", 0, 1))
                assert(not pcall(Network.setDefaultPayloadRateLimit, "example:bad", 1, 0))
                assert(not pcall(Network.setDefaultPayloadRateLimit, "example:bad", 1, 1.5))
                assert(not pcall(Network.setDefaultPayloadRateLimit, "", 1, 1))
            """.trimIndent()), "network_lua_api_test")
            manager.lua.pCall(0, 0)
            assertEquals(PacketRateLimit(0.5, 3), registry.getDefault("example:chat"))
        } finally {
            manager.lua.close()
        }
    }
}
