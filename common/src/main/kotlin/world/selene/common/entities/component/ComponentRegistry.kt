package world.selene.common.entities.component

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class ComponentRegistry(objectMapper: ObjectMapper) : JsonRegistry<ComponentDefinition>(
    objectMapper,
    "common",
    "components",
    ComponentDefinition::class
)