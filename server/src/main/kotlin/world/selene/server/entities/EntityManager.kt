package world.selene.server.entities

import org.koin.mp.KoinPlatform.getKoin
import world.selene.common.data.EntityDefinition
import world.selene.common.lua.LuaReferenceResolver
import world.selene.common.util.Coordinate
import world.selene.server.dimensions.Dimension
import java.util.concurrent.atomic.AtomicInteger

class EntityManager : LuaReferenceResolver<Int, Entity> {
    private val entities = mutableMapOf<Int, Entity>()
    private val nextId = AtomicInteger(1)

    private fun nextEntityId(): Int = nextId.getAndIncrement()

    fun addEntity(entity: Entity) {
        entities.put(entity.networkId, entity)
    }

    fun removeEntity(entity: Entity) {
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
        entity.entityDefinition = entityDefinition
        addEntity(entity)
        return entity
    }

    fun createTransientEntity(entityDefinition: EntityDefinition): Entity {
        val entity = getKoin().get<Entity>()
        entity.networkId = -1
        entity.entityDefinition = entityDefinition
        return entity
    }

    override fun luaDereference(id: Int): Entity? {
        return entities[id]
    }

    fun getEntityByNetworkId(networkId: Int): Entity? {
        return entities[networkId]
    }
}