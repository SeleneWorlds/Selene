package world.selene.common.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class SoundRegistry(objectMapper: ObjectMapper) : JsonRegistry<SoundDefinition>(
    objectMapper,
    "common",
    "sounds",
    SoundDefinition::class
)