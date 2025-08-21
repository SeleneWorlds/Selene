package world.selene.client.animator

import world.selene.client.maps.Entity

class HumanoidAnimator(private val entity: Entity) : Animator {
    override fun getAnimation(): String {
        val animation = if (entity.isInMotion()) "walk" else "stationary"
        return "$animation/${entity.direction.name}"
    }
}