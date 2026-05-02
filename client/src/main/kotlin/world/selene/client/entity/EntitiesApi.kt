package world.selene.client.entity

import world.selene.client.maps.ClientMap
import world.selene.common.entities.EntityDefinition
import world.selene.common.grid.Coordinate

class EntitiesApi(
    private val entityPool: EntityPool,
    private val clientMap: ClientMap
) {

    fun create(entityDefinition: EntityDefinition): EntityApi {
        val entity = entityPool.obtain()
        entity.entityDefinition = entityDefinition.asReference
        return entity.api
    }

    fun getEntitiesAt(coordinate: Coordinate): List<EntityApi> {
        return clientMap.getEntitiesAt(coordinate).map { it.api }
    }

    fun findEntitiesAt(coordinate: Coordinate, tag: String?): List<EntityApi> {
        val result = mutableListOf<EntityApi>()
        for (entity in clientMap.getEntitiesAt(coordinate)) {
            if (tag != null && !entity.hasTag(tag)) {
                continue
            }
            result.add(entity.api)
        }
        return result
    }
}
