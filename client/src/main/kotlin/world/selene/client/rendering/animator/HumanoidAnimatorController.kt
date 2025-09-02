package world.selene.client.rendering.animator

import world.selene.client.maps.Entity

class HumanoidAnimatorController(private val entity: Entity) : AnimatorController {
    override fun getCurrentAnimationName(): String {
        val animation = if (entity.isInMotion()) "walk" else "stationary"
        return "$animation/${entity.direction.name}"
    }
}