package com.seleneworlds.common.tiles.transitions

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class TransitionRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<TransitionDefinition>(
    objectMapper,
    "common",
    "transitions",
    TransitionDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("transitions")
    }
}