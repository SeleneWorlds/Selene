package world.selene.client.entity.component

import org.slf4j.Logger
import world.selene.client.entity.component.rendering.ComponentPositioner
import world.selene.client.entity.component.rendering.IsoVisualComponent
import world.selene.client.entity.component.rendering.Visual2DComponent
import world.selene.client.entity.Entity
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualFactory
import world.selene.client.rendering.visual.VisualRegistry
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.entities.ClientScriptComponentConfiguration
import world.selene.common.entities.ComponentConfiguration
import world.selene.common.entities.VisualComponentConfiguration

class EntityComponentFactory(private val visualRegistry: VisualRegistry, private val visualFactory: VisualFactory, private val logger: Logger) {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is VisualComponentConfiguration -> {
                val context = VisualCreationContext(entity.coordinate, entity.animator, configuration.overrides)
                val positioner = ComponentPositioner.of(configuration.position)
                val visualDef = visualRegistry.get(configuration.visual)
                if (visualDef == null) {
                    logger.error("Failed to create visual: $configuration")
                    return null
                }

                // TODO use ReloadableVisual
                when (val visual = visualFactory.createVisual(visualDef, context)) {
                    is IsoVisual -> {
                        IsoVisualComponent(visual, positioner)
                    }

                    is Visual2D -> {
                        Visual2DComponent(visual, positioner)
                    }

                    else -> {
                        logger.error("Failed to create visual: $configuration")
                        null
                    }
                }
            }

            is ClientScriptComponentConfiguration -> ClientScriptComponent(configuration.script)
            else -> throw IllegalArgumentException("Unknown component configuration: $configuration")
        }
    }
}