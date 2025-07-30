package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper

class TransitionRegistry(objectMapper: ObjectMapper) : JsonRegistry<TransitionDefinition>(
    objectMapper,
    "common",
    "transitions",
    TransitionDefinition::class
)