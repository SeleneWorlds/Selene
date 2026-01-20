package world.selene.common.tiles.transitions

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.Identifier
import world.selene.common.data.json.FileBasedRegistry

class TransitionRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<TransitionDefinition>(
    objectMapper,
    "common",
    "transitions",
    TransitionDefinition::class
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("transitions")
    }
}