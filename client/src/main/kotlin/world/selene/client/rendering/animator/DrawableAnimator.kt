package world.selene.client.rendering.animator

import world.selene.client.rendering.drawable.AnimatedDrawable
import world.selene.client.rendering.drawable.Drawable

class DrawableAnimator : Animator {

    private val animations = mutableMapOf<String, AnimatedDrawable>()

    val drawable: Drawable? get() {
        return null
    }

    fun addAnimation(key: String, drawable: AnimatedDrawable) {
        animations[key] = drawable
    }

}