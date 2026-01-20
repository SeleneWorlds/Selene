package world.selene.common.entities.component

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.FileBasedRegistry

class ComponentRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<ComponentDefinition>(
    objectMapper,
    "common",
    "components",
    ComponentDefinition::class
)