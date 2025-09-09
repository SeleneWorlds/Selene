package world.selene.client.rendering.animator

interface AnimatorController {
    fun getCurrentAnimationName(): String
    fun getCurrentAnimationSpeed(): Float
}