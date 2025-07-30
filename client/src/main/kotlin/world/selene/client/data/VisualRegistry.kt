package world.selene.client.data

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.JsonRegistry

class VisualRegistry(objectMapper: ObjectMapper) : JsonRegistry<VisualDefinition>(
    objectMapper,
    "client",
    "visuals",
    VisualDefinition::class,
)