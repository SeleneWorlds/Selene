package world.selene.client.rendering.animator

import world.selene.client.maps.Entity

interface AnimatorController {
    val currentAnimation: ConfiguredAnimation?
    fun update(entity: Entity, delta: Float)
}