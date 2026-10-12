package com.seleneworlds.server.entities

import com.seleneworlds.common.entities.EntityDefinition

class EntitiesApi(private val entityManager: EntityManager) {

    internal fun subscribeByTag(tag: String, listener: (List<Entity>) -> Unit): () -> Unit =
        entityManager.subscribeByTag(tag, listener)

    internal fun refreshTagCollections() = entityManager.refreshTagCollections()

    fun create(entityDefinition: EntityDefinition): EntityApi {
        return entityManager.createEntity(entityDefinition).api
    }

    fun createTransient(entityDefinition: EntityDefinition): EntityApi {
        return entityManager.createTransientEntity(entityDefinition).api
    }

    fun getByNetworkId(networkId: Int): EntityApi? {
        return entityManager.getEntityByNetworkId(networkId)?.api
    }

    fun getAll(): List<EntityApi> {
        return entityManager.getEntities().map { it.api }
    }

    fun findAllByTag(tag: String): List<EntityApi> {
        return entityManager.findEntitiesByTag(tag).map { it.api }
    }
}
