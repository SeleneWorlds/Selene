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
import com.seleneworlds.common.entities.DraggableComponentConfiguration
import com.seleneworlds.common.entities.GravityComponentConfiguration
import com.seleneworlds.common.entities.ImpassableComponentConfiguration
import com.seleneworlds.common.entities.PassableAboveComponentConfiguration
import com.seleneworlds.common.entities.IgnoresElevationComponentConfiguration
import com.seleneworlds.common.entities.ServerScriptComponentConfiguration
import com.seleneworlds.common.entities.VisualComponentConfiguration
import com.seleneworlds.common.entities.LightComponentConfiguration

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
                    ).also { visual ->
                        visual.initialize()
                    }, positioner
                ).also { component ->
                    component.red = configuration.red
                    component.green = configuration.green
                    component.blue = configuration.blue
                    component.alpha = configuration.alpha
                    component.scale = configuration.scale
                }
            }

            is ClientScriptComponentConfiguration -> ClientScriptComponent(scriptProvider.loadEntityScript(configuration.script))
            is ServerScriptComponentConfiguration -> null
            is GravityComponentConfiguration -> null
            is ImpassableComponentConfiguration -> null
            is PassableAboveComponentConfiguration -> null
            is DraggableComponentConfiguration -> DraggableComponent(configuration.enabled)
            is IgnoresElevationComponentConfiguration -> IgnoresElevationComponent()
            is LightComponentConfiguration -> null
        }
    }
}
