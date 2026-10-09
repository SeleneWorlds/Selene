package com.seleneworlds.server.entities

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import party.iroiro.luajava.Lua
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.attributes.Attribute
import com.seleneworlds.server.attributes.filters.AttributeFilter
import com.seleneworlds.server.collision.CollisionResolver
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.entities.component.EntityComponentFactory
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.script.ServerEntityScript
import com.seleneworlds.server.script.ServerScriptProvider
import com.seleneworlds.server.sync.ChunkViewManager
import com.seleneworlds.server.tiles.transitions.TransitionResolver
import com.seleneworlds.server.world.World
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotSame

class MovementDurationTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json),
        ComponentRegistry(Json), GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )
    private val chunks = ChunkViewManager(TransitionResolver(registries))
    private val dimensions = DimensionManager()
    private val entities = EntityManager()
    private val players = PlayerManager(dimensions, chunks, Json, entities, MainThreadDispatcher())
    private val world = World(Grid(), CollisionResolver(registries, chunks), dimensions, entities, chunks, players)
    private val entity = Entity(registries, world, EntityComponentFactory(object : ServerScriptProvider {
        override fun loadEntityScript(module: String): ServerEntityScript = error("Unused")
    }))

    @Test
    fun `lua exposes movement state and multiplier methods`() {
        val manager = LuaManager(LuaPackageModule())
        try {
            manager.defineMetatable(EntityApi::class, EntityLuaApi.luaMeta)
            val lua = manager.lua
            lua.push(entity.api, Lua.Conversion.NONE)
            lua.setGlobal("entity")
            lua.load(LuaManager.loadBuffer("""
                assert(entity:isMoving() == false)
                assert(entity:getMovementDurationMultiplier() == 1)
                entity:setMovementDurationMultiplier(2)
                assert(entity:getMovementDurationMultiplier() == 2)
            """.trimIndent()), "entity_movement_bindings_test")
            lua.pCall(0, 0)
            entity.dimension = Dimension(registries, world)
            entity.collisionTags.clear()
            assertTrue(entity.moveTo(Coordinate(1, 0, 0), duration = 10f))
            lua.load(LuaManager.loadBuffer("assert(entity:isMoving() == true)"), "entity_moving_test")
            lua.pCall(0, 0)
        } finally {
            manager.lua.close()
        }
    }

    @Test
    fun `entity factory resolves without a clock dependency`() {
        val components = EntityComponentFactory(object : ServerScriptProvider {
            override fun loadEntityScript(module: String): ServerEntityScript = error("Unused")
        })
        val application = koinApplication {
            modules(module {
                single { registries }
                single { world }
                single { components }
                factoryOf(::Entity)
            })
        }
        try {
            val first = application.koin.get<Entity>()
            assertFalse(first.isMoving())
            assertNotSame(first, application.koin.get<Entity>())
        } finally {
            application.close()
        }
    }

    @Test
    fun `movement blocks the next step until its duration ends`() {
        entity.dimension = Dimension(registries, world)
        entity.collisionTags.clear()
        entity.api.setMovementDurationMultiplier(2f)
        assertFalse(entity.api.isMoving())
        assertTrue(entity.moveTo(Coordinate(1, 0, 0)))
        assertTrue(entity.api.isMoving())
        assertFalse(entity.moveTo(Coordinate(2, 0, 0)))
        assertEquals(Coordinate(1, 0, 0), entity.coordinate)
        Thread.sleep(450)
        assertFalse(entity.api.isMoving())
        assertTrue(entity.moveTo(Coordinate(2, 0, 0)))
    }

    @Test
    fun `failed moves do not start movement`() {
        assertFalse(entity.moveTo(Coordinate(1, 0, 0)))
        assertFalse(entity.api.isMoving())
    }

    @Test
    fun `duration uses the current effective multiplier`() {
        assertEquals(0.2f, entity.getMovementDuration(Coordinate.Zero))
        entity.api.setMovementDurationMultiplier(2f)
        assertEquals(0.4f, entity.getMovementDuration(Coordinate.Zero))
        val attribute = entity.attributes.getValue("selene:movement_duration_multiplier")
        var loadMultiplier = 1.5f
        @Suppress("UNCHECKED_CAST")
        (attribute as Attribute<Any?>).addModifier("load", object : AttributeFilter<Any?> {
            override val enabled = true
            override fun apply(attribute: Attribute<Any?>, value: Any?): Any? =
                (value as Number).toFloat() * loadMultiplier
        })
        assertEquals(0.6f, entity.getMovementDuration(Coordinate.Zero))
        loadMultiplier = 2f
        assertEquals(0.8f, entity.getMovementDuration(Coordinate.Zero))
    }

    @Test
    fun `invalid multipliers cannot make movement free or nonfinite`() {
        for (value in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { entity.api.setMovementDurationMultiplier(value) }
            entity.api.createAttribute("selene:movement_duration_multiplier", value)
            assertEquals(0.2f, entity.getMovementDuration(Coordinate.Zero))
        }
    }
}
