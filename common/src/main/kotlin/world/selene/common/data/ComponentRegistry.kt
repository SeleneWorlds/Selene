package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper

class ComponentRegistry(objectMapper: ObjectMapper) : JsonRegistry<ComponentDefinition>(
    objectMapper,
    "common",
    "components",
    ComponentDefinition::class
)