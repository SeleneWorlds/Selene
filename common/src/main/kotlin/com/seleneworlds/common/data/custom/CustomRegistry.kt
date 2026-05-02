package com.seleneworlds.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry
import java.nio.file.Path

class CustomRegistry(
    objectMapper: ObjectMapper,
    definition: CustomRegistryDefinition
) : FileBasedRegistry<CustomRegistryObject>(objectMapper, definition.platform, definition.name, CustomRegistryObject::class) {
    override fun loadEntryFromFile(path: Path, identifier: Identifier): CustomRegistryObject {
        val jsonNode = objectMapper.readTree(path.toFile())
        return CustomRegistryObject(this, identifier, jsonNode)
    }
}
