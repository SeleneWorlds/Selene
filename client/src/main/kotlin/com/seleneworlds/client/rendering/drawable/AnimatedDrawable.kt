package com.seleneworlds.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.seleneworlds.common.event.EventFactory.arrayBackedEvent

class AnimatedDrawable(val frames: List<Drawable>, val duration: Float) : Drawable {
    override val api = AnimatedDrawableApi(this)
    var currentFrame = 0
    var elapsedTime = 0f
    var speed = 1f

    val animationCompleted = arrayBackedEvent<AnimationCompleted> { listeners ->
        AnimationCompleted { listeners.forEach { it.animationCompleted() } }
    }

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return frames.getOrNull(currentFrame)?.getBounds(x, y, outRect) ?: outRect.also { it.set(x, y, 0f, 0f) }
    }

    override fun update(delta: Float) {
        if (frames.size <= 1) return
        elapsedTime += delta * speed
        val frameDuration = duration / frames.size
        while (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrame = (currentFrame + 1) % frames.size
            if (currentFrame == 0) {
                animationCompleted.invoker().animationCompleted()
            }
        }
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        frames.getOrNull(currentFrame)?.render(batch, x, y)
    }

    override fun render(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        frames.getOrNull(currentFrame)?.render(batch, x, y, width, height)
    }

    override fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    ) {
        frames.getOrNull(currentFrame)?.render(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    fun withoutOffset(): AnimatedDrawable {
        return AnimatedDrawable(frames.map {
            if (it is TextureRegionDrawable) it.withoutOffset() else it
        }, duration)
    }

    override fun toString(): String {
        return "AnimatedDrawable(duration=$duration, speed=$speed, currentFrameIndex=$currentFrame, currentFrame=${frames.getOrNull(currentFrame)}, elapsedTime=$elapsedTime)"
    }

    fun interface AnimationCompleted {
        fun animationCompleted()
    }
}
