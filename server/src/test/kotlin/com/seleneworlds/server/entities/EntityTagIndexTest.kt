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

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.bundles.BundleDatabase
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class EntityTagIndexTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json),
        ComponentRegistry(Json), GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )
    private val chunks = ChunkViewManager(TransitionResolver(registries))
    private val dimensions = DimensionManager()
    private val entities = EntityManager()
    private val players = PlayerManager(dimensions, chunks, Json, entities, MainThreadDispatcher())
    private val world = World(Grid(), CollisionResolver(registries, chunks), dimensions, entities, chunks, players)


    private val dimension = Dimension(registries, world)
    private val first = Identifier.parse("test:first")
    private val second = Identifier.parse("test:second")
    private var lookups = 0
    private var listeners = 0
    private val countedRegistry = object : Registry<EntityDefinition> by registries.entities {
        override fun get(identifier: Identifier): EntityDefinition? {
            lookups++
            return registries.entities.get(identifier)
        }
        override fun addReloadListener(listener: RegistryReloadListener<EntityDefinition>) {
            listeners++
            registries.entities.addReloadListener(listener)
        }
        override fun removeReloadListener(listener: RegistryReloadListener<EntityDefinition>) {
            listeners--
            registries.entities.removeReloadListener(listener)
        }
    }

    private fun definition(id: Identifier, vararg tags: String) {
        registries.entities.upsertEntry(id, buildJsonObject {
            put("tags", JsonArray(tags.map(::JsonPrimitive)))
        })
    }

    private fun entity(id: Int, definition: Identifier, spawned: Boolean = true): Entity =
        Entity(registries, world, EntityComponentFactory(object : ServerScriptProvider {
            override fun loadEntityScript(module: String): ServerEntityScript = error("Unused")
        })).also {
            it.networkId = id
            it.entityDefinition = RegistryReference.ByIdentifier(countedRegistry, definition)
            if (spawned) it.dimension = dimension
            entities.addEntity(it)
        }

    private fun compare(vararg tags: String) {
        for (tag in tags) assertEquals(entities.getEntities().filter {
            it.dimension != null && tag in it.entityDefinition.tags
        }, entities.findEntitiesByTag(tag))
    }

    @Test
    fun `lifecycle and definition replacement preserve membership order and query snapshots`() {
        definition(first, "character", "monster")
        definition(second, "character", "npc")
        val a = entity(30, first)
        val b = entity(10, second)
        val c = entity(20, first, spawned = false)
        assertEquals(listOf(a, b), entities.findEntitiesByTag("character"))
        val snapshot = entities.findEntitiesByTag("character")
        a.despawn()
        c.dimension = dimension
        assertEquals(listOf(b, c), entities.findEntitiesByTag("character"))
        a.api.spawn(dimension)
        assertEquals(listOf(a, b, c), entities.findEntitiesByTag("character"))
        assertEquals(listOf(a, b), snapshot)
        a.dimension = Dimension(registries, world)
        a.coordinate = Coordinate(100, -100, 3)
        a.entityDefinition = RegistryReference.ByIdentifier(countedRegistry, second)
        assertEquals(listOf(a, b), entities.findEntitiesByTag("npc"))
        compare("character", "monster", "npc", "absent")
        val replacement = entity(10, first)
        b.entityDefinition = RegistryReference.unbound()
        b.dimension = null // Changes to replaced objects must not change membership.
        assertEquals(listOf(a, replacement, c), entities.findEntitiesByTag("character"))
        a.remove()
        replacement.remove()
        c.remove()
        assertEquals(emptyList(), entities.findEntitiesByTag("character"))
        assertEquals(0, listeners)
    }

    @Test
    fun `entry changes removal readdition and full reload invalidate tags`() {
        definition(first, "old")
        val a = entity(1, first)
        assertEquals(listOf(a), entities.findEntitiesByTag("old"))
        definition(first, "new")
        compare("old", "new")
        assertEquals(listOf(a), entities.findEntitiesByTag("new"))
        registries.entities.removeEntry(first)
        compare("old", "new")
        definition(first, "again")
        assertEquals(listOf(a), entities.findEntitiesByTag("again"))
        registries.entities.load(BundleDatabase())
        assertEquals(emptyList(), entities.findEntitiesByTag("again"))
        definition(first, "restored")
        assertEquals(listOf(a), entities.findEntitiesByTag("restored"))
    }

    @Test
    fun `queries from earlier registry listeners see the published tags`() {
        definition(first, "old")
        var observed: List<Entity>? = null
        registries.entities.addReloadListener(object : RegistryReloadListener<EntityDefinition> {
            override fun onEntryChanged(registry: Registry<EntityDefinition>, identifier: Identifier,
                                        oldData: EntityDefinition, newData: EntityDefinition) {
                observed = entities.findEntitiesByTag("new")
            }
        })
        val a = entity(1, first)
        a.entityDefinition = registries.entities.getReference(first)
        assertEquals(listOf(a), entities.findEntitiesByTag("old"))
        definition(first, "new")
        assertEquals(listOf(a), observed)
        assertEquals(emptyList(), entities.findEntitiesByTag("old"))
    }

    @Test
    fun `repeated queries and coordinate changes do not resolve registry definitions`() {
        definition(first, "character")
        val a = entity(1, first)
        lookups = 0
        repeat(100) {
            a.coordinate = Coordinate(it, 0, 0)
            assertEquals(listOf(a), entities.findEntitiesByTag("character"))
            assertEquals(emptyList(), entities.findEntitiesByTag("absent"))
        }
        assertEquals(0, lookups)
        definition(first, "updated")
        assertEquals(listOf(a), entities.findEntitiesByTag("updated"))
        assertEquals(1, lookups)
        repeat(100) { entities.findEntitiesByTag("updated") }
        assertEquals(1, lookups)
        a.entityDefinition = RegistryReference.unbound()
        assertEquals(emptyList(), entities.findEntitiesByTag("updated"))
        assertEquals(0, listeners)
    }
}
