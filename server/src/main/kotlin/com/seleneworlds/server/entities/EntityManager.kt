package com.seleneworlds.server.entities

import org.koin.mp.KoinPlatform.getKoin
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.util.ReferenceResolver
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.dimensions.Dimension
import java.util.concurrent.atomic.AtomicInteger

class EntityManager : ReferenceResolver<Int, Entity> {
    private val entities = mutableMapOf<Int, Entity>()
    private val spatialIndex = EntitySpatialIndex()
    private val tagIndex = EntityTagIndex()
    private val activeTagIndex = EntityTagIndex { it.dimension != null && it.active }
    private val proximityEntities = linkedSetOf<Entity>()
    private val proximityAwakeEntities = linkedSetOf<Entity>()
    private var maximumSleepRange = 0
    private var maximumZRange = 0
    private var nextActivationUpdate = 0L
    private val activeTickingEntities = linkedSetOf<Entity>()
    private val nextId = AtomicInteger(1)

    private fun nextEntityId(): Int = nextId.getAndIncrement()

    fun addEntity(entity: Entity) {
        entities[entity.networkId]?.takeIf { it !== entity }?.let {
            deactivateEntity(it)
            proximityEntities.remove(it)
            proximityAwakeEntities.remove(it)
        }
        entities[entity.networkId] = entity
        spatialIndex.add(entity)
        tagIndex.add(entity)
        activeTagIndex.add(entity)
        onEntitySleepPolicyChanged(entity)
    }

    fun removeEntity(entity: Entity) {
        deactivateEntity(entity)
        entities.remove(entity.networkId)
        spatialIndex.remove(entity.networkId)
        tagIndex.remove(entity.networkId)
        activeTagIndex.remove(entity.networkId)
        proximityEntities.remove(entity)
        proximityAwakeEntities.remove(entity)
        updateMaximumRanges()
    }

    fun getNearbyEntities(center: Coordinate, dimension: Dimension?, radius: Int): List<Entity> {
        return spatialIndex.nearby(center, dimension, radius)
    }

    fun getEntitiesAt(coordinate: Coordinate, dimension: Dimension?): List<Entity> {
        return spatialIndex.at(coordinate, dimension)
    }

    fun getEntitiesInBox(center: Coordinate, dimension: Dimension?, halfExtents: Coordinate): List<Entity> {
        return spatialIndex.inBox(center, dimension, halfExtents)
    }

    internal fun onEntityLocationChanged(entity: Entity) {
        spatialIndex.update(entity)
        tagIndex.locationChanged(entity)
        onEntityActivityChanged(entity)
    }

    internal fun onEntityDefinitionChanged(entity: Entity) {
        tagIndex.definitionChanged(entity)
        activeTagIndex.definitionChanged(entity)
    }

    fun createEntity(entityDefinition: EntityDefinition): Entity {
        val entity = getKoin().get<Entity>()
        entity.networkId = nextEntityId()
        entity.entityDefinition = entityDefinition.asReference
        entity.loadComponents(entityDefinition)
        addEntity(entity)
        return entity
    }

    fun createTransientEntity(entityDefinition: EntityDefinition): Entity {
        val entity = getKoin().get<Entity>()
        entity.networkId = -1
        entity.entityDefinition = entityDefinition.asReference
        entity.loadComponents(entityDefinition)
        return entity
    }

    fun onEntitySpawned(entity: Entity) {
        if (entity.active && entity.tickableComponents.isNotEmpty()) {
            activeTickingEntities.add(entity)
        }
    }

    fun onEntityDespawned(entity: Entity) {
        deactivateEntity(entity)
    }

    fun update(delta: Float) {
        val now = System.nanoTime()
        if (now >= nextActivationUpdate) {
            refreshActivation()
            nextActivationUpdate = now + 100_000_000L
        }
        activeTickingEntities.toList().forEach { entity ->
            if (entity.dimension == null || !entity.active || entity.tickableComponents.isEmpty()) {
                activeTickingEntities.remove(entity)
                return@forEach
            }
            entity.update(delta)
        }
    }

    override fun dereferencePersisted(id: Int): Entity? {
        return entities[id]
    }

    fun getEntityByNetworkId(networkId: Int): Entity? {
        return entities[networkId]
    }

    fun getEntities(): Collection<Entity> {
        return entities.values
    }

    fun findEntitiesByTag(tag: String): List<Entity> {
        return tagIndex.find(tag)
    }

    internal fun subscribeByTag(tag: String, listener: (List<Entity>) -> Unit): () -> Unit =
        tagIndex.subscribe(tag, listener)

    internal fun subscribeActiveByTag(tag: String, listener: (List<Entity>) -> Unit): () -> Unit =
        activeTagIndex.subscribe(tag, listener)

    internal fun onEntityActivityChanged(entity: Entity) {
        if (entities[entity.networkId] !== entity) return
        activeTagIndex.locationChanged(entity)
        if (entity.dimension != null && entity.active && entity.tickableComponents.isNotEmpty()) {
            activeTickingEntities.add(entity)
        } else activeTickingEntities.remove(entity)
    }

    internal fun onEntitySleepPolicyChanged(entity: Entity) {
        if (entities[entity.networkId] !== entity) return
        if (entity.sleepWhenUnobserved) proximityEntities.add(entity) else {
            proximityEntities.remove(entity)
            proximityAwakeEntities.remove(entity)
            entity.proximityAwake = false
        }
        updateMaximumRanges()
        onEntityActivityChanged(entity)
        nextActivationUpdate = 0L
    }

    private fun updateMaximumRanges() {
        maximumSleepRange = proximityEntities.maxOfOrNull { it.sleepRange } ?: 0
        maximumZRange = proximityEntities.maxOfOrNull { it.activationZRange } ?: 0
    }

    internal fun refreshActivation(observers: Collection<Entity> =
        entities.values.firstOrNull()?.world?.playerManager?.players.orEmpty().mapNotNull { it.controlledEntity }) {
        if (proximityEntities.isEmpty()) return
        val candidates = linkedSetOf<Entity>()
        candidates.addAll(proximityAwakeEntities)
        val players = observers.filter { it.dimension != null }
        for (player in players) {
            spatialIndex.inBox(player.coordinate, player.dimension,
                Coordinate(maximumSleepRange, maximumSleepRange, maximumZRange))
                .filterTo(candidates) { it.sleepWhenUnobserved }
        }
        activeTagIndex.batch {
            for (entity in candidates) {
                val range = if (entity.proximityAwake) entity.sleepRange else entity.wakeRange
                val awake = entity.dimension != null && players.any { player ->
                    player.dimension === entity.dimension &&
                        kotlin.math.abs(player.coordinate.x.toLong() - entity.coordinate.x) <= range &&
                        kotlin.math.abs(player.coordinate.y.toLong() - entity.coordinate.y) <= range &&
                        kotlin.math.abs(player.coordinate.z.toLong() - entity.coordinate.z) <= entity.activationZRange
                }
                if (entity.proximityAwake != awake) {
                    entity.proximityAwake = awake
                    if (awake) proximityAwakeEntities.add(entity) else proximityAwakeEntities.remove(entity)
                    onEntityActivityChanged(entity)
                }
            }
        }
    }

    internal fun refreshTagCollections() {
        tagIndex.refreshDefinitions()
        activeTagIndex.refreshDefinitions()
    }

    private fun deactivateEntity(entity: Entity) {
        activeTickingEntities.remove(entity)
    }
}
