package world.selene.client.rendering.animator

import world.selene.client.entity.Entity

interface AnimatorController {
    val currentAnimation: ConfiguredAnimation?
    fun update(entity: Entity, delta: Float)
}