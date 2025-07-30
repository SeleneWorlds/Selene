package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper

class TileRegistry(objectMapper: ObjectMapper) : JsonRegistry<TileDefinition>(
    objectMapper,
    "common",
    "tiles",
    TileDefinition::class
)