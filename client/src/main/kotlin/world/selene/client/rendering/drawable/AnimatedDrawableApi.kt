package world.selene.client.rendering.drawable

class AnimatedDrawableApi(val animatedDrawable: AnimatedDrawable) : DrawableApi(animatedDrawable) {
    fun getCurrentFrame(): Int {
        return animatedDrawable.currentFrame
    }

    fun getElapsedTime(): Float {
        return animatedDrawable.elapsedTime
    }

    fun getDuration(): Float {
        return animatedDrawable.duration
    }

    fun withoutOffset(): AnimatedDrawableApi {
        return animatedDrawable.withoutOffset().api
    }
}
