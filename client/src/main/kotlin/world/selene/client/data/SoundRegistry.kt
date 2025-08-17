package world.selene.client.data

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.JsonRegistry

class SoundRegistry(objectMapper: ObjectMapper) : JsonRegistry<SoundDefinition>(
    objectMapper,
    "client",
    "sounds",
    SoundDefinition::class,
)
