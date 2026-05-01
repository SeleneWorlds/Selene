package world.selene.client.rendering.drawable

import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkUserdata

object AnimatedDrawableLuaApi {

    /**
     * Current frame index of the animation.
     *
     * ```property
     * CurrentFrame: number
     * ```
     */
    private fun luaGetCurrentFrame(lua: Lua): Int {
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
    private fun luaGetElapsedTime(lua: Lua): Int {
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
    private fun luaGetDuration(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.getDuration())
        return 1
    }

    /**
     * Emitted when the animation completes.
     *
     * ```property
     * AnimationCompleted: Signal
     * ```
     */
    private fun luaGetAnimationCompleted(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.getAnimationCompleted(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Gets a new animated drawable without any offset.
     *
     * ```signatures
     * WithoutOffset() -> AnimatedDrawableApi
     * ```
     */
    private fun luaWithoutOffset(lua: Lua): Int {
        val self = lua.checkUserdata<AnimatedDrawableApi>(1)
        lua.push(self.withoutOffset(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = DrawableLuaApi.luaMeta.extend(AnimatedDrawableApi::class) {
        getter(::luaGetCurrentFrame)
        getter(::luaGetElapsedTime)
        getter(::luaGetDuration)
        getter(::luaGetAnimationCompleted)
        callable(::luaWithoutOffset)
    }
}
