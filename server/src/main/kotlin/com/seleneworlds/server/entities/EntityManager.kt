package com.seleneworlds.server.entities

import org.koin.mp.KoinPlatform.getKoin
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.util.ReferenceResolver
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.dimensions.Dimension
import java.util.concurrent.atomic.AtomicInteger

class EntityManager : ReferenceResolver<Int, Entity> {
    private val entities = mutableMapOf<Int, Entity>()
    private val activeTickingEntities = linkedSetOf<Entity>()
    private val nextId = AtomicInteger(1)

    private fun nextEntityId(): Int = nextId.getAndIncrement()

    fun addEntity(entity: Entity) {
        entities[entity.networkId] = entity
    }

    fun removeEntity(entity: Entity) {
        deactivateEntity(entity)
        entities.remove(entity.networkId)
    }

    fun getNearbyEntities(center: Coordinate, dimension: Dimension?, radius: Int): List<Entity> {
        return entities.values.filter {
            it.dimension == dimension && it.coordinate.horizontalDistanceTo(center) <= radius
        }
    }

    fun getEntitiesAt(coordinate: Coordinate, dimension: Dimension?): List<Entity> {
        return entities.values.filter {
            it.dimension == dimension && it.coordinate == coordinate
        }
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

    private fun deactivateEntity(entity: Entity) {
        activeTickingEntities.remove(entity)
    }
}
