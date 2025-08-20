package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper

class SoundRegistry(objectMapper: ObjectMapper) : JsonRegistry<SoundDefinition>(
    objectMapper,
    "common",
    "sounds",
    SoundDefinition::class
)