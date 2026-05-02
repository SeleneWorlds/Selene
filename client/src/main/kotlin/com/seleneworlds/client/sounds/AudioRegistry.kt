package com.seleneworlds.client.sounds

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class AudioRegistry(json: Json) : FileBasedRegistry<AudioDefinition>(
    json,
    "client",
    "audio",
    AudioDefinition::class,
    AudioDefinition.serializer(),
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("audio")
    }
}
