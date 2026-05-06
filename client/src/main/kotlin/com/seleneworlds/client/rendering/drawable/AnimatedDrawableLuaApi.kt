package com.seleneworlds.client.rendering.drawable

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.xpCall

object AnimatedDrawableLuaApi {

    /**
     * Current frame index of the animation.
     *
     * ```property
     * CurrentFrame: number
     * ```
     */
    private fun getCurrentFrame(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.getCurrentFrame())
        return 1
    }

    /**
     * Time elapsed since the last frame in seconds.
     *
     * ```property
     * ElapsedTime: number
     * ```
     */
    private fun getElapsedTime(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.getElapsedTime())
        return 1
    }

    /**
     * Duration of the full animation in seconds.
     *
     * ```property
     * Duration: number
     * ```
     */
    private fun getDuration(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.getDuration())
        return 1
    }

    /**
     * Emitted when the animation completes.
     *
     * ```property
     * AnimationCompleted: Event
     * ```
     */
    private fun getAnimationCompleted(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        val luaEventSink = LuaEventSink(self.animatedDrawable.animationCompleted) { callback: LuaValue, trace: ScriptTrace ->
            AnimatedDrawable.AnimationCompleted {
                val lua = callback.state()
                lua.push(callback)
                lua.xpCall(0, 0, trace)
            }
        }
        lua.push(luaEventSink, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Gets a new animated drawable without any offset.
     *
     * ```signatures
     * WithoutOffset() -> AnimatedDrawableApi
     * ```
     */
    private fun withoutOffset(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.withoutOffset(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = DrawableLuaApi.luaMeta.extend(AnimatedDrawableApi::class) {
        callable(::getCurrentFrame)
        callable(::getElapsedTime)
        callable(::getDuration)
        callable(::getAnimationCompleted)
        callable(::withoutOffset)
    }
}
