package com.seleneworlds.client.rendering.visual

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.threading.Awaitable

/**
 * Create visuals from visual definitions.
 */
class VisualsApi(
    private val visualRegistry: VisualRegistry,
    private val visualFactory: VisualFactory
) {
    fun create(identifier: Identifier): Awaitable<ReloadableVisualApi> {
        val visualDef = requireNotNull(visualRegistry.get(identifier)) { "Visual not found: $identifier" }
        return Awaitable.completed(
            ReloadableVisual.Instance(visualFactory, visualDef.asReference, VisualCreationContext()).api
        )
    }
}
