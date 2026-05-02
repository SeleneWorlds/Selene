package com.seleneworlds.common.tiles.transitions

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class TransitionRegistry(json: Json) : FileBasedRegistry<TransitionDefinition>(
    json,
    "common",
    "transitions",
    TransitionDefinition::class,
    TransitionDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("transitions")
    }
}
