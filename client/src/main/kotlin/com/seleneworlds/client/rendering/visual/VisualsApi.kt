package com.seleneworlds.client.rendering.visual

import com.seleneworlds.common.data.Identifier

/**
 * Create visuals from visual definitions.
 */
class VisualsApi(
    private val visualRegistry: VisualRegistry,
    private val visualFactory: VisualFactory
) {
    fun create(identifier: Identifier): ReloadableVisualApi {
        val visualDef = requireNotNull(visualRegistry.get(identifier)) { "Visual not found: $identifier" }
        return ReloadableVisual.Instance(visualFactory, visualDef.asReference, VisualCreationContext()).api
    }
}
