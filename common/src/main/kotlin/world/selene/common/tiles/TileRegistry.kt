package world.selene.common.tiles

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.Identifier
import world.selene.common.data.json.FileBasedRegistry

class TileRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<TileDefinition>(
    objectMapper,
    "common",
    "tiles",
    TileDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("tiles")
    }
}