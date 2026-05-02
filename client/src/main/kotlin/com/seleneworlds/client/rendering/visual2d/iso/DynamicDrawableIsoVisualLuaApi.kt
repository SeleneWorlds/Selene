package com.seleneworlds.client.rendering.visual2d.iso

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata

object DynamicDrawableIsoVisualLuaApi {

    /**
     * Surface height of the visual, i.e. the offset applied to other visuals on top of it.
     *
     * ```property
     * SurfaceHeight: number
     * ```
     */
    private fun luaGetSurfaceHeight(lua: Lua): Int {
        val self = lua.checkUserdata<DynamicDrawableIsoVisualApi>(1)
        lua.push(self.getSurfaceHeight())
        return 1
    }

    /**
     * Drawable rendered by this visual (this frame).
     *
     * ```property
     * Drawable: DrawableApi
     * ```
     */
    private fun luaGetDrawable(lua: Lua): Int {
        val self = lua.checkUserdata<DynamicDrawableIsoVisualApi>(1)
        lua.push(self.getDrawable(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = LuaMappedMetatable(DynamicDrawableIsoVisualApi::class) {
        getter(::luaGetSurfaceHeight)
        getter(::luaGetDrawable)
    }
}
