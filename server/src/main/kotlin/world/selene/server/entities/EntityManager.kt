package world.selene.server.entities

import org.koin.mp.KoinPlatform.getKoin
import world.selene.server.dimensions.Dimension
import world.selene.common.util.Coordinate
import java.util.concurrent.atomic.AtomicInteger

class EntityManager {
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

    fun getAll(): Collection<Entity> = entities.values

    fun createEntity(entityType: String): Entity {
        val entity = getKoin().get<Entity>()
        entity.networkId = nextEntityId()
        entity.entityType = entityType
        addEntity(entity)
        return entity
    }

    fun createTransientEntity(entityType: String): Entity {
        val entity = getKoin().get<Entity>()
        entity.networkId = -1
        entity.entityType = entityType
        return entity
    }
}