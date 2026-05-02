package com.seleneworlds.common.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class SoundRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<SoundDefinition>(
    objectMapper,
    "common",
    "sounds",
    SoundDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("sounds")
    }
}