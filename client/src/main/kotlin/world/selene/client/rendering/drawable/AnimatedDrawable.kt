package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.Signal

class AnimatedDrawable(val frames: List<Drawable>, val duration: Float) : Drawable {
    private var currentFrame = 0
    private var elapsedTime = 0f

    private val animationCompleted = Signal("AnimationCompleted")

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return frames.getOrNull(currentFrame)?.getBounds(x, y, outRect) ?: outRect.also { it.set(x, y, 0f, 0f) }
    }

    override fun update(delta: Float) {
        if (frames.size <= 1) return
        elapsedTime += delta
        val frameDuration = duration
        while (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrame = (currentFrame + 1) % frames.size
            if (currentFrame == 0) {
                animationCompleted.emit()
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

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = Drawable.luaMeta.extend(AnimatedDrawable::class) {
            readOnly(AnimatedDrawable::currentFrame)
            readOnly(AnimatedDrawable::elapsedTime)
            readOnly(AnimatedDrawable::duration)
            readOnly(AnimatedDrawable::animationCompleted)
        }
    }
}