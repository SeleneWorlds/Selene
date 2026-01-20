package world.selene.common.entities

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.Identifier
import world.selene.common.data.json.FileBasedRegistry

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