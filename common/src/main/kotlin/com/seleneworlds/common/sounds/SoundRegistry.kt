package com.seleneworlds.common.sounds

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class SoundRegistry(json: Json) : FileBasedRegistry<SoundDefinition>(
    json,
    "common",
    "sounds",
    SoundDefinition::class,
    SoundDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("sounds")
    }
}
