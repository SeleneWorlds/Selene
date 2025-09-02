package world.selene.client.rendering

import world.selene.client.maps.Entity

interface TickableComponent {
    fun update(entity: Entity, delta: Float)
}