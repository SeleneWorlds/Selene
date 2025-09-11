package world.selene.common.entities

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class EntityRegistry(objectMapper: ObjectMapper) : JsonRegistry<EntityDefinition>(
    objectMapper,
    "common",
    "entities",
    EntityDefinition::class
)