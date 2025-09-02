package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.Signal

class AnimatedDrawable(val frames: List<Drawable>, val duration: Float) : Drawable, LuaMetatableProvider {
    private var currentFrame = 0
    private var elapsedTime = 0f

    private val animationCompleted = Signal("AnimationCompleted")

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

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(AnimatedDrawable::class) {
            readOnly(AnimatedDrawable::currentFrame)
            readOnly(AnimatedDrawable::elapsedTime)
            readOnly(AnimatedDrawable::animationCompleted)
        }
    }
}