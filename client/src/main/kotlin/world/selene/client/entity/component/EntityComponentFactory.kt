package world.selene.client.entity.component

import world.selene.client.maps.ClientScriptComponent
import world.selene.client.maps.Entity
import world.selene.client.maps.EntityComponent
import world.selene.client.maps.IsoVisualComponent
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualManager
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.data.ClientScriptComponentConfiguration
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.VisualComponentConfiguration

class EntityComponentFactory(private val visualManager: VisualManager) {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is VisualComponentConfiguration -> {
                val context = VisualCreationContext(entity.coordinate, entity.animator)
                val visual = visualManager.createVisual(configuration.visual, context)
                if (visual is IsoVisual) {
                    IsoVisualComponent(visual)
                } else null
            }

            is ClientScriptComponentConfiguration -> ClientScriptComponent(configuration.script)
            else -> throw IllegalArgumentException("Unknown component configuration: $configuration")
        }
    }
}