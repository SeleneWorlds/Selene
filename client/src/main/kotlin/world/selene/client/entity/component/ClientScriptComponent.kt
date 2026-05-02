package world.selene.client.entity.component

import world.selene.client.entity.Entity
import world.selene.client.script.ClientEntityScript
import world.selene.common.observable.ObservableMap

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
