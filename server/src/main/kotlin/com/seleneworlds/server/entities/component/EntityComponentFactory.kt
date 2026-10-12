package com.seleneworlds.server.entities.component

import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.ImpassableComponentConfiguration
import com.seleneworlds.common.entities.GravityComponentConfiguration
import com.seleneworlds.common.entities.PassableAboveComponentConfiguration
import com.seleneworlds.server.entities.Entity

class EntityComponentFactory {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is GravityComponentConfiguration -> GravityComponent()
            is ImpassableComponentConfiguration -> ImpassableComponent(configuration.enabled)
            is PassableAboveComponentConfiguration -> PassableAboveComponent(configuration.enabled)
            else -> null
        }
    }
}
