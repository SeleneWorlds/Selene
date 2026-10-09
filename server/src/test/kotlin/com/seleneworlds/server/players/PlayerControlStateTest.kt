package com.seleneworlds.server.players

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.network.NetworkClient
import com.seleneworlds.server.sync.ChunkViewManager
import com.seleneworlds.server.tiles.transitions.TransitionResolver
import kotlinx.serialization.json.Json
import party.iroiro.luajava.Lua
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerControlStateTest {
    private val client = object : NetworkClient {
        override val writable = true
        override val address = InetSocketAddress("localhost", 0)
        override fun send(packet: Packet) {}
        override fun disconnect() {}
        override fun poll(): Packet? = null
        override fun enqueueWork(runnable: Runnable) { runnable.run() }
    }
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json),
        ComponentRegistry(Json), GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )
    private val players = PlayerManager(
        DimensionManager(), ChunkViewManager(TransitionResolver(registries)),
        Json, EntityManager(), MainThreadDispatcher()
    )
    private val player = players.createPlayer(client)

    @Test
    fun `Lua exposes independent persistent control states`() {
        val manager = LuaManager(LuaPackageModule())
        try {
            manager.defineMetatable(PlayerApi::class, PlayerLuaApi.luaMeta)
            manager.lua.push(player.api, Lua.Conversion.NONE)
            manager.lua.setGlobal("player")
            manager.lua.load(LuaManager.loadBuffer("""
                assert(player:canMove() and player:canTurn())
                player:setCanMove(false)
                assert(not player:canMove() and player:canTurn())
                player:setCanTurn(false)
                assert(not player:canMove() and not player:canTurn())
                player:setCanMove(true)
                assert(player:canMove() and not player:canTurn())
                player:setCanTurn(true)
                assert(player:canMove() and player:canTurn())
                controlChecksCompleted = true
            """.trimIndent()), "player_control_state_test")
            manager.lua.pCall(0, 0)
            manager.lua.getGlobal("controlChecksCompleted")
            assertTrue(manager.lua.toBoolean(-1))
        } finally {
            manager.lua.close()
        }
    }

    @Test
    fun `disabled movement does not expire or consume cooldown`() {
        player.api.setCanMove(false)
        assertFalse(player.acquireMoveRequestCooldown(0.5f, 0))
        assertFalse(player.acquireMoveRequestCooldown(0.5f, 60_000_000_000))
        assertFalse(player.api.canMove())
        player.api.setCanMove(true)
        assertTrue(player.acquireMoveRequestCooldown(0.5f, 60_000_000_000))
        assertFalse(player.acquireMoveRequestCooldown(0.5f, 60_499_999_999))
        assertTrue(player.acquireMoveRequestCooldown(0.5f, 60_500_000_000))
    }

    @Test
    fun `enabling movement preserves existing cooldown and turning is independent`() {
        assertTrue(player.acquireMoveRequestCooldown(2f, 0))
        player.api.setCanMove(false)
        player.api.setCanMove(true)
        player.api.setCanTurn(false)
        assertFalse(player.api.canTurn())
        assertFalse(player.acquireMoveRequestCooldown(1f, 1_000_000_000))
        assertTrue(player.acquireMoveRequestCooldown(1f, 2_000_000_000))
        player.api.setCanTurn(true)
        assertTrue(player.api.canTurn())
    }
}
