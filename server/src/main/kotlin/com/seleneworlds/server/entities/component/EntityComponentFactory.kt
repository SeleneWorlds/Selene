package com.seleneworlds.server.entities.component

import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.ServerScriptComponentConfiguration
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.script.ServerScriptProvider

class EntityComponentFactory(
    private val scriptProvider: ServerScriptProvider
) {
    fun create(entity: Entity, configuration: ComponentConfiguration): EntityComponent? {
        return when (configuration) {
            is ServerScriptComponentConfiguration -> ServerScriptComponent(scriptProvider.loadEntityScript(configuration.script))
            else -> null
        }
    }
}
