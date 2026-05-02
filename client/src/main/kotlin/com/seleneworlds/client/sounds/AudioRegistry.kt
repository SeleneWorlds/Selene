package com.seleneworlds.client.sounds

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class AudioRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<AudioDefinition>(
    objectMapper,
    "client",
    "audio",
    AudioDefinition::class,
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("audio")
    }
}