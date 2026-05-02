package com.seleneworlds.common.entities

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class EntityRegistry(json: Json) : FileBasedRegistry<EntityDefinition>(
    json,
    "common",
    "entities",
    EntityDefinition::class,
    EntityDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("entities")
    }
}
