package com.seleneworlds.client.rendering.visual

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class VisualRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<VisualDefinition>(
    objectMapper,
    "client",
    "visuals",
    VisualDefinition::class,
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("visuals")
    }
}