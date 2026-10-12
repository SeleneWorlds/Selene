package com.seleneworlds.server.entities

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
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
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntitySpatialIndexTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json),
        ComponentRegistry(Json), GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )
    private val chunks = ChunkViewManager(TransitionResolver(registries))
    private val dimensions = DimensionManager()
    private val entities = EntityManager()
    private val players = PlayerManager(dimensions, chunks, Json, entities, MainThreadDispatcher())
    private val world = World(Grid(), CollisionResolver(registries, chunks), dimensions, entities, chunks, players)

    private fun entity(id: Int, coordinate: Coordinate, dimension: Dimension?): Entity {
        return Entity(registries, world, EntityComponentFactory(object : ServerScriptProvider {
            override fun loadEntityScript(module: String): ServerEntityScript = error("Unused")
        })).also {
            it.networkId = id
            it.coordinate = coordinate
            it.dimension = dimension
            entities.addEntity(it)
        }
    }

    private fun compare(center: Coordinate, dimension: Dimension?, radius: Int) {
        assertEquals(entities.getEntities().filter {
            it.dimension == dimension && it.coordinate.horizontalDistanceTo(center) <= radius
        }, entities.getNearbyEntities(center, dimension, radius))
        assertEquals(entities.getEntities().filter {
            it.dimension == dimension && it.coordinate == center
        }, entities.getEntitiesAt(center, dimension))
    }

    @Test
    fun `queries match full scan across boundaries floors dimensions and extreme coordinates`() {
        val first = Dimension(registries, world)
        val second = Dimension(registries, world)
        val random = kotlin.random.Random(17)
        repeat(1500) { id ->
            entity(id, Coordinate(random.nextInt(-200, 201), random.nextInt(-200, 201), id % 3),
                when (id % 3) { 0 -> first; 1 -> second; else -> null })
        }
        entity(2000, Coordinate(-16, -16, 5), first)
        entity(2001, Coordinate(1, 1, 9), first) // Truncated distance is 1, regardless of floor.
        for (dimension in listOf(first, second, null)) {
            for (center in listOf(Coordinate.Zero, Coordinate(-16, -16, 5), Coordinate(16, 16, 0))) {
                for (radius in listOf(-1, 0, 1, 15, 16, 17, 32, 500, Int.MAX_VALUE)) {
                    compare(center, dimension, radius)
                }
            }
        }
        entity(2002, Coordinate(Int.MIN_VALUE, 0, 0), first)
        compare(Coordinate(Int.MAX_VALUE, 0, 0), first, 1)
        compare(Coordinate.Zero, first, Int.MAX_VALUE)
    }

    @Test
    fun `movement teleport gravity lifecycle and replacement keep index and order current`() {
        val first = Dimension(registries, world)
        val second = Dimension(registries, world)
        val a = entity(10, Coordinate.Zero, first)
        val b = entity(3, Coordinate(32, 0, 0), first)
        val c = entity(20, Coordinate.Zero, first)
        a.collisionTags.clear()
        assertTrue(a.moveTo(Coordinate(16, 0, 0), duration = 0f))
        compare(Coordinate.Zero, first, 0)
        compare(a.coordinate, first, 0)
        a.api.setCoordinate(Coordinate(32, 0, 0))
        assertEquals(listOf(a, b), entities.getEntitiesAt(a.coordinate, first))
        a.fallOneTile()
        compare(Coordinate(32, 0, 0), first, 0)
        compare(a.coordinate, first, 0)
        a.api.spawn(second)
        compare(a.coordinate, first, 0)
        compare(a.coordinate, second, 0)
        a.despawn()
        compare(a.coordinate, second, 0)
        compare(a.coordinate, null, 0)
        a.api.spawn(first)
        a.remove()
        compare(a.coordinate, first, 0)
        val replacement = entity(3, Coordinate.Zero, first)
        assertEquals(listOf(replacement, c), entities.getEntitiesAt(Coordinate.Zero, first))
        b.coordinate = Coordinate(500, 500, 0) // Replaced object must not update its replacement.
        compare(Coordinate.Zero, first, 0)
        val transient = Entity(registries, world, EntityComponentFactory(object : ServerScriptProvider {
            override fun loadEntityScript(module: String): ServerEntityScript = error("Unused")
        }))
        transient.coordinate = Coordinate.Zero
        transient.dimension = first
        compare(Coordinate.Zero, first, 0)
    }
}
