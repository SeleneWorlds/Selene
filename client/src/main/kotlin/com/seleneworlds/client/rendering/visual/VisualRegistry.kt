package com.seleneworlds.client.rendering.visual

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class VisualRegistry(json: Json) : FileBasedRegistry<VisualDefinition>(
    json,
    "client",
    "visuals",
    VisualDefinition::class,
    VisualDefinition.serializer(),
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("visuals")
    }
}
