package world.selene.client.rendering.drawable

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.Signal

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

    fun getAnimationCompleted(): Signal {
        return animatedDrawable.animationCompleted
    }

    fun withoutOffset(): AnimatedDrawableApi {
        return animatedDrawable.withoutOffset().api
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return AnimatedDrawableLuaApi.luaMeta
    }
}
