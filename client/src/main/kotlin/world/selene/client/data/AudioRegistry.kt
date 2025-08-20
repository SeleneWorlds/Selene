package world.selene.client.data

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.JsonRegistry

class AudioRegistry(objectMapper: ObjectMapper) : JsonRegistry<AudioDefinition>(
    objectMapper,
    "client",
    "audio",
    AudioDefinition::class,
)
