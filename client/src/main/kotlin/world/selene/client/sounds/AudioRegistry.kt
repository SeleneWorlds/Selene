package world.selene.client.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.FileBasedRegistry

class AudioRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<AudioDefinition>(
    objectMapper,
    "client",
    "audio",
    AudioDefinition::class,
)
