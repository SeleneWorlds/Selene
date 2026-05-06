package com.seleneworlds.server.entities.component

import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.script.ServerEntityScript

class ServerScriptComponent(
    private val script: ServerEntityScript
) : EntityComponent, TickableComponent {
    private var initialized = false
    private var enabled = true
    private val data = ObservableMap()

    override fun update(entity: Entity, delta: Float) {
        if (!enabled) return
        if (!initialized) {
            if (!script.initialize(entity.api, data)) {
                enabled = false
                return
            }
            initialized = true
        }
        if (!script.tick(entity.api, data, delta)) {
            enabled = false
        }
    }
}
