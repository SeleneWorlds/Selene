package com.seleneworlds.common.tiles

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class TileRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<TileDefinition>(
    objectMapper,
    "common",
    "tiles",
    TileDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("tiles")
    }
}