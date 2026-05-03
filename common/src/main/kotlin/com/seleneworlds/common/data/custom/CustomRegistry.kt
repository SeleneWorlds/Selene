package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class CustomRegistry(
    json: Json,
    definition: CustomRegistryDefinition
) : FileBasedRegistry<CustomRegistryObject>(
    json,
    definition.platform,
    definition.name,
    CustomRegistryObject::class
    ) {
    override fun loadEntryFromElement(element: JsonElement, identifier: Identifier): CustomRegistryObject {
        return CustomRegistryObject(this, identifier, element)
    }
}
