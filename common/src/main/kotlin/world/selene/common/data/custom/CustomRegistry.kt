package world.selene.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.Identifier
import world.selene.common.data.json.FileBasedRegistry
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
