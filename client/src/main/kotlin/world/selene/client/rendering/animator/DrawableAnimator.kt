package world.selene.client.rendering.animator

import world.selene.client.rendering.drawable.AnimatedDrawable
import world.selene.client.rendering.drawable.Drawable

class DrawableAnimator(private val controller: AnimatorController) {

    private val animations = mutableMapOf<String, AnimatedDrawable>()

    val drawable: Drawable?
        get() {
            val currentAnimation = controller.currentAnimation
            return currentAnimation?.let { animations[it.name] }
                ?.also { it.speed = currentAnimation.speed }
        }

    fun addAnimation(key: String, drawable: AnimatedDrawable) {
        animations[key] = drawable
    }

}