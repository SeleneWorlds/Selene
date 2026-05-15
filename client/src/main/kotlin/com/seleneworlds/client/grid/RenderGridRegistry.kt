package com.seleneworlds.client.grid

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class RenderGridRegistry(json: Json) : FileBasedRegistry<RenderGridDefinition>(
    json,
    "client",
    "render_grids",
    RenderGridDefinition::class,
    RenderGridDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("render_grids")
    }
}
