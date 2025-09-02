package world.selene.client.rendering.animator

import world.selene.client.rendering.drawable.AnimatedDrawable
import world.selene.client.rendering.drawable.Drawable

class DrawableAnimator(private val controller: AnimatorController) : Animator {

    private val animations = mutableMapOf<String, AnimatedDrawable>()

    val drawable: Drawable? get() {
        return controller.getCurrentAnimationName().let { animations[it] }
    }

    fun addAnimation(key: String, drawable: AnimatedDrawable) {
        animations[key] = drawable
    }

}