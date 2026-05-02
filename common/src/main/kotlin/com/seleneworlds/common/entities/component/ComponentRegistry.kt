package com.seleneworlds.common.entities.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class ComponentRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<ComponentDefinition>(
    objectMapper,
    "common",
    "components",
    ComponentDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace( "components")
    }
}