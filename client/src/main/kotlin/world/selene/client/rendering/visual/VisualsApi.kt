package world.selene.client.rendering.visual

import world.selene.common.data.Identifier

/**
 * Create visuals from visual definitions.
 */
@Suppress("SameReturnValue")
class VisualsApi(
    private val visualRegistry: VisualRegistry,
    private val visualFactory: VisualFactory
) {
    fun create(identifier: Identifier): ReloadableVisualApi {
        val visualDef = requireNotNull(visualRegistry.get(identifier)) { "Visual not found: $identifier" }
        return ReloadableVisual.Instance(visualFactory, visualDef.asReference, VisualCreationContext()).api
    }
}
