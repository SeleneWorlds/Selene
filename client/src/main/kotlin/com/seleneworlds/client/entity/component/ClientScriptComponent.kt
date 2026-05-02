package com.seleneworlds.client.entity.component

import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.script.ClientEntityScript
import com.seleneworlds.common.observable.ObservableMap

class ClientScriptComponent(
    private val script: ClientEntityScript
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
