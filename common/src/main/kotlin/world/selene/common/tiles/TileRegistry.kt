package world.selene.common.tiles

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.json.JsonRegistry

class TileRegistry(objectMapper: ObjectMapper) : JsonRegistry<TileDefinition>(
    objectMapper,
    "common",
    "tiles",
    TileDefinition::class
)