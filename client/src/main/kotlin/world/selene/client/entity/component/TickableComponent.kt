package world.selene.client.entity.component

import world.selene.client.entity.Entity

interface TickableComponent {
    fun update(entity: Entity, delta: Float)
}