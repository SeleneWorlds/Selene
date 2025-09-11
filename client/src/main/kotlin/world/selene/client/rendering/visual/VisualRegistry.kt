package world.selene.client.rendering.visual

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class VisualRegistry(objectMapper: ObjectMapper) : JsonRegistry<VisualDefinition>(
    objectMapper,
    "client",
    "visuals",
    VisualDefinition::class,
)