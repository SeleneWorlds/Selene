package world.selene.client.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class AudioRegistry(objectMapper: ObjectMapper) : JsonRegistry<AudioDefinition>(
    objectMapper,
    "client",
    "audio",
    AudioDefinition::class,
)
