package world.selene.common.tiles.transitions

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class TransitionRegistry(objectMapper: ObjectMapper) : JsonRegistry<TransitionDefinition>(
    objectMapper,
    "common",
    "transitions",
    TransitionDefinition::class
)