package com.seleneworlds.common.grid

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class GridRegistry(json: Json) : FileBasedRegistry<GridDefinition>(
    json,
    "common",
    "grids",
    GridDefinition::class,
    GridDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("grids")
    }
}
