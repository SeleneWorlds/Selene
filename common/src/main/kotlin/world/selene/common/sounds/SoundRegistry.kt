package world.selene.common.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.FileBasedRegistry

class SoundRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<SoundDefinition>(
    objectMapper,
    "common",
    "sounds",
    SoundDefinition::class
)