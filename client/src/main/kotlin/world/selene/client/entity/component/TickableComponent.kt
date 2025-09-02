package world.selene.client.entity.component

import world.selene.client.maps.Entity

interface TickableComponent {
    fun update(entity: Entity, delta: Float)
}