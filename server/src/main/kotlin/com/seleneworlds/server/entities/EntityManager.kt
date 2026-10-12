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
    private val activeTickingEntities = linkedSetOf<Entity>()
    private val nextId = AtomicInteger(1)

    private fun nextEntityId(): Int = nextId.getAndIncrement()

    fun addEntity(entity: Entity) {
        entities[entity.networkId] = entity
        spatialIndex.add(entity)
        tagIndex.add(entity)
    }

    fun removeEntity(entity: Entity) {
        deactivateEntity(entity)
        entities.remove(entity.networkId)
        spatialIndex.remove(entity.networkId)
        tagIndex.remove(entity.networkId)
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
    }

    internal fun onEntityDefinitionChanged(entity: Entity) {
        tagIndex.definitionChanged(entity)
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
        if (entity.tickableComponents.isNotEmpty()) {
            activeTickingEntities.add(entity)
        }
    }

    fun onEntityDespawned(entity: Entity) {
        deactivateEntity(entity)
    }

    fun update(delta: Float) {
        activeTickingEntities.toList().forEach { entity ->
            if (entity.dimension == null || entity.tickableComponents.isEmpty()) {
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

    private fun deactivateEntity(entity: Entity) {
        activeTickingEntities.remove(entity)
    }
}
