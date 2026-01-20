package world.selene.client.rendering.visual

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.data.Identifier
import world.selene.common.data.json.FileBasedRegistry

class VisualRegistry(objectMapper: ObjectMapper) : FileBasedRegistry<VisualDefinition>(
    objectMapper,
    "client",
    "visuals",
    VisualDefinition::class,
) {
    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("visuals")
    }
}