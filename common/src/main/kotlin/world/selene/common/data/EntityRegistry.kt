package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper

class EntityRegistry(objectMapper: ObjectMapper) : JsonRegistry<EntityDefinition>(
    objectMapper,
    "common",
    "entities",
    EntityDefinition::class
)