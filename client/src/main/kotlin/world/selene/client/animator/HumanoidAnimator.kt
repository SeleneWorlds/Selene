package world.selene.client.animator

import world.selene.client.maps.Entity

class HumanoidAnimator(private val entity: Entity) : Animator {
    override fun getAnimation(): String {
        var animation = if (entity.isInMotion()) "walk" else "idle"
        //animation = "walk"
        return "$animation/${entity.facing.name}"
    }
}