package com.seleneworlds.common.tiles

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class TileRegistry(json: Json) : FileBasedRegistry<TileDefinition>(
    json,
    "common",
    "tiles",
    TileDefinition::class,
    TileDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("tiles")
    }
}
