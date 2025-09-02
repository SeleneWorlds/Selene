package world.selene.client.entity.component

import world.selene.client.maps.Entity
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualManager
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.data.ClientScriptComponentConfiguration
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.VisualComponentConfiguration

class EntityComponentFactory(private val visualManager: VisualManager) {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is VisualComponentConfiguration -> {
                val context = VisualCreationContext(entity.coordinate, entity.animator, configuration.overrides)
                val positioner = ComponentPositioner.of(configuration.position)
                when (val visual = visualManager.createVisual(configuration.visual, context)) {
                    is IsoVisual -> {
                        IsoVisualComponent(visual, positioner)
                    }

                    is Visual2D -> {
                        Visual2DComponent(visual, positioner)
                    }

                    else -> null
                }
            }

            is ClientScriptComponentConfiguration -> ClientScriptComponent(configuration.script)
            else -> throw IllegalArgumentException("Unknown component configuration: $configuration")
        }
    }
}