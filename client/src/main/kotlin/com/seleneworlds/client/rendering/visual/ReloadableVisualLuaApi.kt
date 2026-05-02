package com.seleneworlds.client.rendering.visual

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata

object ReloadableVisualLuaApi {

    /**
     * Surface height of the visual, i.e. the offset applied to other visuals on top of it.
     *
     * ```property
     * SurfaceHeight: number
     * ```
     */
    private fun luaGetSurfaceHeight(lua: Lua): Int {
        val self = lua.checkUserdata<ReloadableVisualApi>(1)
        lua.push(self.getSurfaceHeight())
        return 1
    }

    /**
     * Drawable rendered by this visual, or nil if this visual is not backed by a Drawable.
     *
     * ```property
     * Drawable: DrawableApi
     * ```
     */
    private fun luaGetDrawable(lua: Lua): Int {
        val self = lua.checkUserdata<ReloadableVisualApi>(1)
        self.getDrawable()?.let {
            lua.push(it, Lua.Conversion.NONE)
        } ?: lua.pushNil()
        return 1
    }

    val luaMeta = LuaMappedMetatable(ReloadableVisualApi::class) {
        getter(::luaGetSurfaceHeight)
        getter(::luaGetDrawable)
    }
}
