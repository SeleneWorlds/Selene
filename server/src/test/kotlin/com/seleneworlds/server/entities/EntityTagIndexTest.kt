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
import com.seleneworlds.server.sync.ChunkViewManager
import com.seleneworlds.server.tiles.transitions.TransitionResolver
import com.seleneworlds.server.world.World
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable

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
    @Test
    fun `sleeping and disabled entities stop component ticking and overrides resume it`() {
        definition(first, "character")
        val monster = entity(1, first)
        var ticks = 0
        monster.tickableComponents.add(object : com.seleneworlds.server.entities.component.TickableComponent {
            override fun update(entity: Entity, delta: Float) { ticks++ }
        })
        entities.onEntitySpawned(monster)
        entities.update(0.1f)
        assertEquals(1, ticks)
        monster.configureSleeping(true)
        entities.update(0.1f)
        assertEquals(1, ticks)
        monster.keepAwake = true
        entities.update(0.1f)
        assertEquals(2, ticks)
        monster.enabled = false
        entities.update(0.1f)
        assertEquals(2, ticks)
        monster.enabled = true
        entities.update(0.1f)
        assertEquals(3, ticks)
        monster.despawn()
        entities.update(0.1f)
        assertEquals(3, ticks)
        monster.api.spawn(dimension)
        entities.update(0.1f)
        assertEquals(4, ticks)
    }

    @Test
    fun `proximity sleeping respects hysteresis floors dimensions and enabled state`() {
        definition(first, "character")
        val monster = entity(1, first)
        val observer = entity(2, first)
        monster.configureSleeping(true)
        val updates = mutableListOf<List<Entity>>()
        val unsubscribe = entities.subscribeActiveByTag("character") { updates.add(it) }
        try {
            assertEquals(false, monster.awake)
            assertEquals(listOf(observer), updates.last())
            observer.coordinate = Coordinate(60, 0, 2)
            entities.refreshActivation(listOf(observer))
            assertEquals(true, monster.active)
            observer.coordinate = Coordinate(70, 0, 2)
            entities.refreshActivation(listOf(observer))
            assertEquals(true, monster.awake)
            observer.coordinate = Coordinate(71, 0, 2)
            entities.refreshActivation(listOf(observer))
            assertEquals(false, monster.awake)
            observer.coordinate = Coordinate(65, 0, 0)
            entities.refreshActivation(listOf(observer))
            assertEquals(false, monster.awake)
            observer.coordinate = Coordinate(0, 0, 3)
            entities.refreshActivation(listOf(observer))
            assertEquals(false, monster.awake)
            observer.coordinate = Coordinate(0, 0, 0)
            observer.dimension = Dimension(registries, world)
            entities.refreshActivation(listOf(observer))
            assertEquals(false, monster.awake)
            observer.dimension = dimension
            entities.refreshActivation(listOf(observer))
            monster.enabled = false
            assertEquals(true, monster.awake)
            assertEquals(false, monster.active)
            assertEquals(listOf(observer), updates.last())
            monster.enabled = true
            assertEquals(listOf(monster, observer), updates.last())
            entities.refreshActivation(emptyList())
            assertEquals(false, monster.active)
            monster.keepAwake = true
            assertEquals(true, monster.active)
            monster.enabled = false
            assertEquals(false, monster.active)
            monster.enabled = true
            monster.keepAwake = false
            assertEquals(false, monster.active)
            monster.configureSleeping(false)
            assertEquals(true, monster.active)
            monster.despawn()
            assertEquals(listOf(observer), updates.last())
        } finally { unsubscribe() }
    }

    @Test
    fun `waking a population publishes once and steady idle passes do not look up definitions`() {
        definition(first, "character")
        val population = (1..2000).map { entity(it, first).also { e -> e.configureSleeping(true) } }
        val observer = entity(2001, first)
        var publications = 0
        var active = emptyList<Entity>()
        val unsubscribe = entities.subscribeActiveByTag("character") { publications++; active = it }
        try {
            assertEquals(listOf(observer), active)
            lookups = 0
            repeat(100) { entities.refreshActivation(emptyList()) }
            assertEquals(0, lookups)
            assertEquals(1, publications)
            entities.refreshActivation(listOf(observer))
            assertEquals(population + observer, active)
            assertEquals(2, publications)
            entities.refreshActivation(emptyList())
            assertEquals(listOf(observer), active)
            assertEquals(3, publications)
            lookups = 0
            repeat(100) { entities.refreshActivation(emptyList()) }
            assertEquals(3, publications)
            assertEquals(0, lookups)
        } finally { unsubscribe() }
    }

    @Test
    fun `Lua active collection retains identity and updates on script flags and registry changes`() {
        definition(first, "character")
        val entity = entity(1, first)
        val manager = LuaManager(LuaPackageModule())
        val module = EntitiesLuaApi(EntitiesApi(entities), registries)
        fun run(script: String) {
            manager.lua.load(LuaManager.loadBuffer(script), "active_collection_test")
            manager.lua.pCall(0, 0)
            assertEquals(0, manager.lua.top)
        }
        try {
            module.initialize(manager)
            manager.lua.push(manager.lua.newTable { module.register(this) })
            manager.lua.setGlobal("Entities")
            run("all = Entities.byTag('character'); active = Entities.activeByTag('character'); e = active[1]; assert(e:isActive() and e:isAwake() and e:isEnabled())")
            run("e:setSleepWhenUnobserved(true); assert(#active == 0 and #all == 1 and not e:isAwake())")
            run("e:setKeepAwake(true); assert(#active == 1 and active[1]:getNetworkId() == e:getNetworkId()); e:setEnabled(false); assert(#active == 0 and e:isAwake())")
            run("e:setEnabled(true); assert(#active == 1); assert(rawequal(active, Entities.activeByTag('character')))")
            definition(first, "other")
            run("assert(#active == 0)")
            definition(first, "character")
            run("assert(#active == 1)")
            entity.remove()
            run("assert(#active == 0 and #all == 0)")
        } finally {
            module.dispose()
            manager.lua.close()
        }
    }

    @Test
    fun `Lua byTag retains table and userdata identity while lifecycle and reload update membership`() {
        definition(first, "character")
        definition(second, "other")
        val manager = LuaManager(LuaPackageModule())
        val module = EntitiesLuaApi(EntitiesApi(entities), registries)
        fun run(script: String) {
            manager.lua.load(LuaManager.loadBuffer(script), "entity_collection_test")
            manager.lua.pCall(0, 0)
            assertEquals(0, manager.lua.top)
        }
        try {
            module.initialize(manager)
            manager.lua.push(manager.lua.newTable { module.register(this) })
            manager.lua.setGlobal("Entities")
            run("characters = Entities.byTag('character'); assert(#characters == 0)")
            val a = entity(30, first)
            run("assert(#characters == 1); original = characters[1]; snapshot = Entities.findAllByTag('character')")
            val b = entity(10, first)
            run("assert(rawequal(characters, Entities.byTag('character'))); assert(#characters == 2); assert(rawequal(original, characters[1])); other = characters[2]; assert(#snapshot == 1)")
            a.despawn()
            run("assert(#characters == 1); assert(rawequal(other, characters[1]))")
            a.api.spawn(dimension)
            run("assert(#characters == 2); assert(rawequal(other, characters[2]))")
            a.entityDefinition = RegistryReference.ByIdentifier(countedRegistry, second)
            run("assert(#characters == 1); assert(rawequal(other, characters[1]))")
            definition(first, "other")
            run("assert(#characters == 0)")
            definition(first, "character")
            run("assert(#characters == 1)")
            b.remove()
            run("assert(#characters == 0)")
            a.remove()
            entity(20, first)
            run("assert(#characters == 1)")
            module.dispose()
            run("assert(#characters == 0)")
        } finally {
            module.dispose()
            manager.lua.close()
        }
    }

    @Test
    fun `Lua tag collections are independent across states and tags`() {
        definition(first, "character", "monster")
        val a = entity(1, first)
        val firstState = party.iroiro.luajava.lua54.Lua54()
        val secondState = party.iroiro.luajava.lua54.Lua54()
        val collection = EntityCollection(firstState)
        val other = EntityCollection(secondState)
        val unsubscribe = entities.subscribeByTag("character", collection::update)
        val unsubscribeOther = entities.subscribeByTag("monster", other::update)
        try {
            collection.push(firstState)
            other.push(secondState)
            definition(first, "character")
            firstState.rawGetI(1, 1)
            secondState.rawGetI(1, 1)
            assertEquals(a.api, firstState.toJavaObject(-1))
            assertEquals(true, secondState.isNil(-1))
        } finally {
            unsubscribe()
            unsubscribeOther()
            collection.close()
            other.close()
            firstState.close()
            secondState.close()
        }
    }
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
        Entity(registries, world, EntityComponentFactory()).also {
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
