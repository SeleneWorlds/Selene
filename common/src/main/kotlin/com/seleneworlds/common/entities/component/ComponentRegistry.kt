package com.seleneworlds.common.entities.component

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class ComponentRegistry(json: Json) : FileBasedRegistry<ComponentDefinition>(
    json,
    "common",
    "components",
    ComponentDefinition::class,
    ComponentDefinition.serializer()
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace( "components")
    }
}
