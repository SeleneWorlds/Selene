package world.selene.server.entities

import world.selene.common.entities.EntityDefinition

class EntitiesApi(private val entityManager: EntityManager) {

    fun create(entityDefinition: EntityDefinition): EntityApi {
        return entityManager.createEntity(entityDefinition).api
    }

    fun createTransient(entityDefinition: EntityDefinition): EntityApi {
        return entityManager.createTransientEntity(entityDefinition).api
    }

    fun getByNetworkId(networkId: Int): EntityApi? {
        return entityManager.getEntityByNetworkId(networkId)?.api
    }
}
