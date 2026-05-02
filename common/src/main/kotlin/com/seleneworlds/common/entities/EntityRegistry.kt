package com.seleneworlds.common.entities

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class EntityRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<EntityDefinition>(
    objectMapper,
    "common",
    "entities",
    EntityDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("entities")
    }
}