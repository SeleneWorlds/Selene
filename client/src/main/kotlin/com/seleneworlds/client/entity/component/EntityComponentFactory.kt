package com.seleneworlds.client.entity.component

import org.slf4j.Logger
import com.seleneworlds.client.entity.component.rendering.ComponentPositioner
import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.entity.component.rendering.ReloadableVisualComponent
import com.seleneworlds.client.rendering.visual.ReloadableVisual
import com.seleneworlds.client.rendering.visual.VisualCreationContext
import com.seleneworlds.client.rendering.visual.VisualFactory
import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.client.script.ClientScriptProvider
import com.seleneworlds.common.entities.ClientScriptComponentConfiguration
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.VisualComponentConfiguration

class EntityComponentFactory(
    private val visualRegistry: VisualRegistry,
    private val visualFactory: VisualFactory,
    private val scriptProvider: ClientScriptProvider,
    private val logger: Logger
) {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is VisualComponentConfiguration -> {
                val context = VisualCreationContext(entity.coordinate, entity.animator, configuration.overrides)
                val positioner = ComponentPositioner.of(configuration.position)
                val visualDef = visualRegistry.get(configuration.visual)
                if (visualDef == null) {
                    logger.error("Visual definition not found: ${configuration.visual}")
                    return null
                }

                ReloadableVisualComponent(
                    ReloadableVisual.Instance(
                        visualFactory,
                        visualDef.asReference,
                        context
                    ), positioner
                )
            }

            is ClientScriptComponentConfiguration -> ClientScriptComponent(scriptProvider.loadEntityScript(configuration.script))
            else -> throw IllegalArgumentException("Unknown component configuration: $configuration")
        }
    }
}
