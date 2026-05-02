package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry
import com.seleneworlds.common.serialization.decodeFromFile
import java.nio.file.Path

class CustomRegistry(
    json: Json,
    definition: CustomRegistryDefinition
) : FileBasedRegistry<CustomRegistryObject>(
    json,
    definition.platform,
    definition.name,
    CustomRegistryObject::class
    ) {
    override fun loadEntryFromFile(path: Path, identifier: Identifier): CustomRegistryObject {
        val element = json.decodeFromFile(JsonElement.serializer(), path)
        return CustomRegistryObject(this, identifier, element)
    }
}
